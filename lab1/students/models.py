from django.db import models


class Subject(models.Model):
    name = models.CharField(max_length=100, verbose_name="Предмет")

    class Meta:
        verbose_name = "Предмет"
        verbose_name_plural = "Предмети"

    def __str__(self):
        return self.name


class Student(models.Model):
    full_name = models.CharField(max_length=200, verbose_name="ПІБ")
    group = models.CharField(max_length=50, verbose_name="Група")
    photo = models.ImageField(
        upload_to="students/photos/", blank=True, null=True, verbose_name="Фото"
    )
    phone = models.CharField(max_length=20, blank=True, verbose_name="Телефон")
    email = models.EmailField(blank=True, verbose_name="Email")

    class Meta:
        verbose_name = "Студент"
        verbose_name_plural = "Студенти"
        ordering = ["full_name"]

    def __str__(self):
        return f"{self.full_name} ({self.group})"

    def average_grade(self):
        grades = self.grades.all()
        if not grades:
            return None
        return round(sum(g.value for g in grades) / grades.count(), 2)


class Grade(models.Model):
    student = models.ForeignKey(
        Student, on_delete=models.CASCADE, related_name="grades", verbose_name="Студент"
    )
    subject = models.ForeignKey(
        Subject, on_delete=models.CASCADE, related_name="grades", verbose_name="Предмет"
    )
    value = models.DecimalField(max_digits=4, decimal_places=1, verbose_name="Оцінка")

    class Meta:
        verbose_name = "Оцінка"
        verbose_name_plural = "Оцінки"
        unique_together = ("student", "subject")

    def __str__(self):
        return f"{self.student} — {self.subject}: {self.value}"
