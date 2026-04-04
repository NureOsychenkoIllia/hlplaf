import base64
import csv
import io

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
from django.contrib import messages
from django.db.models import Avg, Count
from django.http import HttpResponse
from django.shortcuts import get_object_or_404, redirect, render

from .forms import CSVImportForm, GradeFormSet, StudentForm
from .models import Grade, Student, Subject

# ── Level 1: student list ────────────────────────────────────────────────────


def student_list(request):
    group_filter = request.GET.get("group", "")
    students = Student.objects.prefetch_related("grades__subject").all()
    if group_filter:
        students = students.filter(group__icontains=group_filter)
    subjects = Subject.objects.all()
    groups = (
        Student.objects.values_list("group", flat=True).distinct().order_by("group")
    )
    return render(
        request,
        "students/student_list.html",
        {
            "students": students,
            "subjects": subjects,
            "groups": groups,
            "group_filter": group_filter,
        },
    )


def student_detail(request, pk):
    student = get_object_or_404(Student, pk=pk)
    grades = student.grades.select_related("subject").all()
    return render(
        request,
        "students/student_detail.html",
        {
            "student": student,
            "grades": grades,
        },
    )


# ── Level 2: add / edit / delete + photo/contact ────────────────────────────


def student_add(request):
    if request.method == "POST":
        form = StudentForm(request.POST, request.FILES)
        formset = GradeFormSet(request.POST)
        if form.is_valid() and formset.is_valid():
            student = form.save()
            formset.instance = student
            formset.save()
            messages.success(request, "Студента додано успішно.")
            return redirect("student_list")
    else:
        form = StudentForm()
        formset = GradeFormSet()
    return render(
        request,
        "students/student_form.html",
        {"form": form, "formset": formset, "title": "Додати студента"},
    )


def student_edit(request, pk):
    student = get_object_or_404(Student, pk=pk)
    if request.method == "POST":
        form = StudentForm(request.POST, request.FILES, instance=student)
        formset = GradeFormSet(request.POST, instance=student)
        if form.is_valid() and formset.is_valid():
            form.save()
            formset.save()
            messages.success(request, "Дані студента оновлено.")
            return redirect("student_detail", pk=pk)
    else:
        form = StudentForm(instance=student)
        formset = GradeFormSet(instance=student)
    return render(
        request,
        "students/student_form.html",
        {"form": form, "formset": formset, "title": "Редагувати студента"},
    )


def student_delete(request, pk):
    student = get_object_or_404(Student, pk=pk)
    if request.method == "POST":
        student.delete()
        messages.success(request, "Студента видалено.")
        return redirect("student_list")
    return render(request, "students/student_confirm_delete.html", {"student": student})


# ── Level 3: CSV import ──────────────────────────────────────────────────────


def import_csv(request):
    if request.method == "POST":
        form = CSVImportForm(request.POST, request.FILES)
        if form.is_valid():
            csv_file = form.cleaned_data["csv_file"]
            try:
                decoded = csv_file.read().decode("utf-8-sig")
                reader = csv.DictReader(io.StringIO(decoded))
                created = 0
                for row in reader:
                    full_name = (row.get("ПІБ") or row.get("full_name") or "").strip()
                    group = (row.get("Група") or row.get("group") or "").strip()
                    if not full_name or not group:
                        continue
                    student, _ = Student.objects.get_or_create(
                        full_name=full_name,
                        group=group,
                        defaults={
                            "phone": (
                                row.get("Телефон") or row.get("phone") or ""
                            ).strip(),
                            "email": (
                                row.get("Email") or row.get("email") or ""
                            ).strip(),
                        },
                    )
                    skip_keys = {
                        "ПІБ",
                        "Група",
                        "Телефон",
                        "Email",
                        "full_name",
                        "group",
                        "phone",
                        "email",
                        "Середня оцінка",
                    }
                    for key, val in row.items():
                        if key in skip_keys or not val.strip():
                            continue
                        subject, _ = Subject.objects.get_or_create(name=key.strip())
                        try:
                            Grade.objects.update_or_create(
                                student=student,
                                subject=subject,
                                defaults={"value": float(val.strip())},
                            )
                        except ValueError:
                            pass
                    created += 1
                messages.success(request, f"Імпортовано {created} студентів.")
            except Exception as exc:
                messages.error(request, f"Помилка імпорту: {exc}")
            return redirect("student_list")
    else:
        form = CSVImportForm()
    example = "full_name,group,phone,email,Математика,Фізика\nІваненко Іван Іванович,ІТ-21,0501234567,ivan@example.com,90,85"
    return render(
        request, "students/import_csv.html", {"form": form, "example": example}
    )


# ── Level 4: reports & charts ────────────────────────────────────────────────


def _chart_to_b64(fig):
    buf = io.BytesIO()
    fig.savefig(buf, format="png", bbox_inches="tight")
    plt.close(fig)
    buf.seek(0)
    return base64.b64encode(buf.read()).decode()


def report(request):
    subjects = Subject.objects.annotate(
        avg=Avg("grades__value"), cnt=Count("grades")
    ).order_by("name")
    groups = (
        Student.objects.values("group")
        .annotate(avg=Avg("grades__value"), count=Count("id"))
        .order_by("group")
    )

    # Bar chart: average grade per subject
    fig1, ax1 = plt.subplots(figsize=(8, 4))
    names = [s.name for s in subjects]
    avgs = [float(s.avg) if s.avg else 0 for s in subjects]
    bars = ax1.bar(names, avgs, color="#4c7fa6")
    ax1.set_ylabel("Середня оцінка")
    ax1.set_title("Середня оцінка за предметами")
    ax1.set_ylim(0, 100)
    for bar, val in zip(bars, avgs):
        ax1.text(
            bar.get_x() + bar.get_width() / 2,
            bar.get_height() + 1,
            f"{val:.1f}",
            ha="center",
            va="bottom",
            fontsize=9,
        )
    plt.xticks(rotation=20, ha="right")
    chart_subjects = _chart_to_b64(fig1)

    # Bar chart: average grade per group
    fig2, ax2 = plt.subplots(figsize=(8, 4))
    group_names = [g["group"] for g in groups]
    group_avgs = [float(g["avg"]) if g["avg"] else 0 for g in groups]
    bars2 = ax2.bar(group_names, group_avgs, color="#6aaa64")
    ax2.set_ylabel("Середня оцінка")
    ax2.set_title("Середня оцінка за групами")
    ax2.set_ylim(0, 100)
    for bar, val in zip(bars2, group_avgs):
        ax2.text(
            bar.get_x() + bar.get_width() / 2,
            bar.get_height() + 1,
            f"{val:.1f}",
            ha="center",
            va="bottom",
            fontsize=9,
        )
    chart_groups = _chart_to_b64(fig2)

    # Table data: each student with all subject grades
    all_subjects = list(Subject.objects.all())
    students = Student.objects.prefetch_related("grades__subject").all()
    table_rows = []
    for s in students:
        grade_map = {g.subject_id: g.value for g in s.grades.all()}
        row_grades = [grade_map.get(sub.id, "—") for sub in all_subjects]
        table_rows.append(
            {"student": s, "grades": row_grades, "avg": s.average_grade()}
        )

    return render(
        request,
        "students/report.html",
        {
            "subjects": subjects,
            "groups": groups,
            "chart_subjects": chart_subjects,
            "chart_groups": chart_groups,
            "all_subjects": all_subjects,
            "table_rows": table_rows,
        },
    )


def export_csv(request):
    subjects = list(Subject.objects.all())
    students = Student.objects.prefetch_related("grades__subject").all()
    response = HttpResponse(content_type="text/csv; charset=utf-8")
    response["Content-Disposition"] = 'attachment; filename="students_report.csv"'
    response.write("\ufeff")  # BOM for Excel
    writer = csv.writer(response)
    header = (
        ["ПІБ", "Група", "Телефон", "Email"]
        + [s.name for s in subjects]
        + ["Середня оцінка"]
    )
    writer.writerow(header)
    for student in students:
        grade_map = {g.subject_id: g.value for g in student.grades.all()}
        row = [student.full_name, student.group, student.phone, student.email]
        row += [grade_map.get(sub.id, "") for sub in subjects]
        row.append(student.average_grade() or "")
        writer.writerow(row)
    return response
