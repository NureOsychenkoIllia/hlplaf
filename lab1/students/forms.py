from django import forms
from .models import Student, Grade, Subject


class StudentForm(forms.ModelForm):
    class Meta:
        model = Student
        fields = ["full_name", "group", "photo", "phone", "email"]
        widgets = {
            "full_name": forms.TextInput(attrs={"class": "form-control"}),
            "group": forms.TextInput(attrs={"class": "form-control"}),
            "phone": forms.TextInput(attrs={"class": "form-control"}),
            "email": forms.EmailInput(attrs={"class": "form-control"}),
        }


class GradeForm(forms.ModelForm):
    class Meta:
        model = Grade
        fields = ["subject", "value"]
        widgets = {
            "subject": forms.Select(attrs={"class": "form-select"}),
            "value": forms.NumberInput(attrs={"class": "form-control", "step": "0.1", "min": "0", "max": "100"}),
        }


GradeFormSet = forms.inlineformset_factory(
    Student,
    Grade,
    form=GradeForm,
    extra=3,
    can_delete=True,
)


class CSVImportForm(forms.Form):
    csv_file = forms.FileField(
        label="CSV-файл",
        widget=forms.FileInput(attrs={"class": "form-control", "accept": ".csv"}),
    )
