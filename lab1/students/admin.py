from django.contrib import admin
from .models import Subject, Student, Grade


class GradeInline(admin.TabularInline):
    model = Grade
    extra = 1


@admin.register(Student)
class StudentAdmin(admin.ModelAdmin):
    list_display = ("full_name", "group", "email", "phone", "average_grade")
    search_fields = ("full_name", "group", "email")
    list_filter = ("group",)
    inlines = [GradeInline]


@admin.register(Subject)
class SubjectAdmin(admin.ModelAdmin):
    list_display = ("name",)


@admin.register(Grade)
class GradeAdmin(admin.ModelAdmin):
    list_display = ("student", "subject", "value")
    list_filter = ("subject",)
