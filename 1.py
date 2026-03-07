# Рівень 1
# 4.    Напишіть функцію, яка приймає три параметри (a, b, c) і виводить на екран найменше з них.
def find_min(a, b, c):
    minimum = min(a, b, c)
    print(f"Найменше число: {minimum}")

find_min(5, 3, 8)

# Рівень 2
# 4.	Напишіть функцію, яка приймає рядок та повертає його обернений варіант. Наприклад, "hello" повинно повернути "olleh".
def reverse(string):
    reversed_string = string[::-1]
    return reversed_string

print(reverse("hello"))

# Рівень 3
# 4.	Реалізуйте програму, яка визначає, чи є слово паліндромом (читається однаково з обох боків).
def is_palindrome(word):
    lowered_word = word.lower()
    return lowered_word == lowered_word[::-1]

print(is_palindrome("радар"))
print(is_palindrome("hello"))

# Рівень 4
# 4.	Створіть клас "Книготека" з можливістю додавання та видалення книг, а також виведення списку усіх книг.
class Library:
    def __init__(self):
        self.books = []

    def add_book(self, book):
        self.books.append(book)

    def remove_book(self, book):
        if book in self.books:
            self.books.remove(book)

    def display_books(self):
        print("Книги: " + ", ".join(self.books))

Library = Library()
Library.add_book("тест1")
Library.add_book("тест2")
Library.display_books()