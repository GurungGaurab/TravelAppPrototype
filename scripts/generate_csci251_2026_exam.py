from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    ListFlowable,
    ListItem,
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)


OUTPUT_PATH = "output/pdf/CSCI251_2026_Mock_Exam.pdf"


def build_styles():
    styles = getSampleStyleSheet()
    styles.add(
        ParagraphStyle(
            name="TitleCenter",
            parent=styles["Title"],
            fontName="Helvetica-Bold",
            fontSize=16,
            leading=20,
            alignment=TA_CENTER,
            spaceAfter=6,
        )
    )
    styles.add(
        ParagraphStyle(
            name="MetaCenter",
            parent=styles["Normal"],
            fontName="Helvetica-Bold",
            fontSize=11,
            leading=14,
            alignment=TA_CENTER,
            spaceAfter=4,
        )
    )
    styles.add(
        ParagraphStyle(
            name="SectionHeading",
            parent=styles["Heading2"],
            fontName="Helvetica-Bold",
            fontSize=12,
            leading=15,
            textColor=colors.HexColor("#1F3B5C"),
            spaceBefore=8,
            spaceAfter=6,
        )
    )
    styles.add(
        ParagraphStyle(
            name="Question",
            parent=styles["Normal"],
            fontName="Helvetica-Bold",
            fontSize=10.5,
            leading=14,
            spaceBefore=6,
            spaceAfter=4,
        )
    )
    styles.add(
        ParagraphStyle(
            name="BodyTight",
            parent=styles["Normal"],
            fontName="Helvetica",
            fontSize=10,
            leading=13,
            spaceAfter=4,
            alignment=TA_LEFT,
        )
    )
    styles.add(
        ParagraphStyle(
            name="Small",
            parent=styles["Normal"],
            fontName="Helvetica",
            fontSize=9,
            leading=11,
            spaceAfter=3,
        )
    )
    styles.add(
        ParagraphStyle(
            name="ExamCode",
            parent=styles["Normal"],
            fontName="Courier",
            fontSize=8.5,
            leading=10.5,
            leftIndent=8,
            spaceAfter=5,
        )
    )
    return styles


def p(text, style):
    return Paragraph(text.replace("\n", "<br/>"), style)


def bullet_list(items, style, bullet_type="1"):
    return ListFlowable(
        [ListItem(p(item, style)) for item in items],
        bulletType=bullet_type,
        start="1",
        leftIndent=18,
    )


def code_block(text, styles):
    escaped = (
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace(" ", "&nbsp;")
    )
    return p(escaped, styles["ExamCode"])


def add_header_footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 9)
    canvas.setFillColor(colors.grey)
    canvas.drawString(doc.leftMargin, 15 * mm, "2026 Hong Kong Campus CSCI251 Mock Examination")
    canvas.drawRightString(A4[0] - doc.rightMargin, 15 * mm, str(canvas.getPageNumber()))
    canvas.restoreState()


def build_story():
    styles = build_styles()
    story = []

    story.append(p("School of Computing and Information Technology", styles["MetaCenter"]))
    story.append(p("CSCI251 Advanced Programming", styles["TitleCenter"]))
    story.append(p("Hong Kong Campus", styles["MetaCenter"]))
    story.append(p("Mock Examination Paper - Spring Session 2026", styles["MetaCenter"]))
    story.append(Spacer(1, 6))

    info_table = Table(
        [
            ["Family name", "", "Student number", ""],
            ["Other names", "", "Table number", ""],
            ["Exam duration", "3 hours", "Mode of Exam", "Face-to-Face"],
        ],
        colWidths=[32 * mm, 58 * mm, 32 * mm, 48 * mm],
    )
    info_table.setStyle(
        TableStyle(
            [
                ("GRID", (0, 0), (-1, -1), 0.5, colors.grey),
                ("BACKGROUND", (0, 0), (-1, -1), colors.whitesmoke),
                ("FONTNAME", (0, 0), (-1, -1), "Helvetica"),
                ("FONTNAME", (0, 2), (0, 2), "Helvetica-Bold"),
                ("FONTNAME", (2, 2), (2, 2), "Helvetica-Bold"),
                ("FONTNAME", (0, 0), (0, 1), "Helvetica-Bold"),
                ("FONTNAME", (2, 0), (2, 1), "Helvetica-Bold"),
                ("FONTSIZE", (0, 0), (-1, -1), 9),
                ("LEADING", (0, 0), (-1, -1), 11),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("PADDING", (0, 0), (-1, -1), 4),
            ]
        )
    )
    story.append(info_table)
    story.append(Spacer(1, 8))

    directions = [
        "This exam contains 17 questions in 2 parts, for a total of 50 marks.",
        "Attempt ALL questions in Part A and Part B.",
        "Put all answers for Part A on ONE page.",
        "Start a new page for each question in Part B.",
        "Non-programmable calculators are permitted. No electronic communication is allowed.",
        "Write clear, standard C++ code where a programming answer is required.",
    ]
    story.append(p("Directions to students", styles["SectionHeading"]))
    story.append(bullet_list(directions, styles["BodyTight"]))

    story.append(Spacer(1, 6))
    story.append(p("PART A: Attempt all questions. Each question carries 0.5 mark.", styles["SectionHeading"]))
    story.append(
        p(
            "Choose the best answer for each question. Put all your answers for Part A on ONE page and number them clearly.",
            styles["BodyTight"],
        )
    )

    mcqs = [
        (
            "1. Which statement about <b>unique_ptr</b> is TRUE?",
            [
                "A. It allows multiple owners of the same object.",
                "B. It automatically deletes the managed object when ownership ends.",
                "C. It must always point to an array.",
                "D. It cannot be moved.",
                "E. It can only manage stack memory.",
            ],
        ),
        (
            "2. What is the main reason for declaring a destructor as <b>virtual</b> in a base class?",
            [
                "A. To prevent object slicing.",
                "B. To allow constructor overloading.",
                "C. To ensure the derived-class destructor runs when deleting through a base pointer.",
                "D. To make the class abstract automatically.",
                "E. To improve compile-time performance.",
            ],
        ),
        (
            "3. Consider <font face='Courier'>int x = 4; int&amp; r = x; r += 3;</font>. What is the final value of <font face='Courier'>x</font>?",
            ["A. 3", "B. 4", "C. 7", "D. Undefined", "E. Compilation error"],
        ),
        (
            "4. Which C++20 feature is primarily used to constrain template parameters with readable rules?",
            ["A. ranges", "B. concepts", "C. modules", "D. coroutines", "E. constexpr destructors"],
        ),
        (
            "5. Which operator is typically overloaded to allow output using <font face='Courier'>cout &lt;&lt; obj</font>?",
            ["A. +", "B. =", "C. []", "D. &lt;&lt;", "E. ()"],
        ),
        (
            "6. Which statement about exception handling is FALSE?",
            [
                "A. A catch block can handle a thrown object of a matching type.",
                "B. Stack unwinding occurs after an exception is thrown.",
                "C. Code after a throw statement in the same block is always executed.",
                "D. Catching by reference can avoid unnecessary copying.",
                "E. Exceptions can separate error handling from normal logic.",
            ],
        ),
        (
            "7. Which of the following best describes object slicing?",
            [
                "A. Copying a derived object into a base object by value and losing derived parts.",
                "B. Deleting an object through a pointer.",
                "C. Passing a reference to a function.",
                "D. Splitting a class across header and source files.",
                "E. Using multiple inheritance.",
            ],
        ),
        (
            "8. What does the following return type mean: <font face='Courier'>optional&lt;string&gt;</font>?",
            [
                "A. The function returns a string and a bool.",
                "B. The function always returns an empty string.",
                "C. The function may contain a string value or no value at all.",
                "D. The function returns multiple strings.",
                "E. The function returns a raw pointer to a string.",
            ],
        ),
        (
            "9. What is the output of <font face='Courier'>cout &lt;&lt; (5 &lt;=&gt; 2 &gt; 0);</font> ?",
            ["A. 0", "B. 1", "C. 2", "D. -1", "E. Compilation error"],
        ),
        (
            "10. Which container is most suitable when a class needs to store an expandable list of student names?",
            ["A. int*", "B. array&lt;string, 3&gt;", "C. vector&lt;string&gt;", "D. pair&lt;string, string&gt;", "E. nullptr"],
        ),
    ]
    for question, options in mcqs:
        story.append(p(question, styles["Question"]))
        for option in options:
            story.append(p(option, styles["BodyTight"]))

    story.append(PageBreak())
    story.append(p("PART B: Attempt ALL questions. Each question weighs differently.", styles["SectionHeading"]))

    story.append(p("11. Evaluate or identify the issue in each independent code segment.", styles["Question"]))
    story.append(p("A. State the final value of <font face='Courier'>x</font>.", styles["BodyTight"]))
    story.append(code_block("int x = 8;\nx -= 3;\nx *= 2;\nx /= 5;\nx += 7;", styles))
    story.append(p("(1 mark)", styles["Small"]))
    story.append(p("B. State the final value of <font face='Courier'>x</font> and explain why.", styles["BodyTight"]))
    story.append(code_block("int a = 9;\nint& x = a;\nx = x - a / 3;", styles))
    story.append(p("(1 mark)", styles["Small"]))
    story.append(p("C. State the output or the type of error, if any.", styles["BodyTight"]))
    story.append(code_block("void update(int value) {\n    value += 10;\n}\nint main() {\n    int x = 5;\n    update(x);\n    cout << x;\n}", styles))
    story.append(p("(1 mark)", styles["Small"]))
    story.append(p("D. State the output or the type of error, if any.", styles["BodyTight"]))
    story.append(code_block("int main() {\n    int a = 0, b = 4, x;\n    if (a = b) {\n        x = 100;\n    } else {\n        x = 200;\n    }\n    cout << x;\n}", styles))
    story.append(p("(1 mark)", styles["Small"]))
    story.append(p("E. State the output of the following code.", styles["BodyTight"]))
    story.append(code_block("int main() {\n    auto cmp = 7 <=> 7;\n    cout << (cmp == 0 ? \"same\" : \"diff\");\n}", styles))
    story.append(p("(1 mark)", styles["Small"]))

    story.append(p("12. Write a program that reads a student's score and prints the corresponding grade using a <font face='Courier'>switch</font> statement where possible.", styles["Question"]))
    story.append(
        bullet_list(
            [
                "Prompt the user to enter an integer score from 0 to 100.",
                "Print <font face='Courier'>A</font> for 90-100, <font face='Courier'>B</font> for 80-89, <font face='Courier'>C</font> for 70-79, <font face='Courier'>D</font> for 60-69, and <font face='Courier'>F</font> for below 60.",
                "Reject invalid input outside 0 to 100 with an appropriate message.",
                "Use integer division or another suitable strategy together with <font face='Courier'>switch</font>.",
            ],
            styles["BodyTight"],
        )
    )
    story.append(p("(3 marks)", styles["Small"]))

    story.append(p("13. Write a C++ template function called <font face='Courier'>countGreaterThan</font>.", styles["Question"]))
    story.append(
        bullet_list(
            [
                "The function takes an array, its size, and a threshold value.",
                "Return how many elements are greater than the threshold.",
                "The function should work for arrays of different types such as <font face='Courier'>int</font>, <font face='Courier'>double</font>, and <font face='Courier'>char</font>.",
                "Write one short example call in <font face='Courier'>main()</font>.",
            ],
            styles["BodyTight"],
        )
    )
    story.append(p("(4 marks)", styles["Small"]))

    story.append(p("14. Implement class <font face='Courier'>LibraryBook</font> according to the following class diagram.", styles["Question"]))
    diagram = Table(
        [
            [p("<b>LibraryBook</b>", styles["BodyTight"])],
            [p("- bookID: int<br/>- title: string<br/>- author: string<br/>- vector&lt;string&gt; borrowers", styles["BodyTight"])],
            [
                p(
                    "+ LibraryBook(bookID:int, title:string, author:string)<br/>"
                    "+ getBookID() const: int<br/>"
                    "+ setBookID(bookID:int): void<br/>"
                    "+ getTitle() const: string<br/>"
                    "+ setTitle(title:string): void<br/>"
                    "+ borrowBook(studentName:string): void<br/>"
                    "+ displayInfo(): void",
                    styles["BodyTight"],
                )
            ],
        ],
        colWidths=[160 * mm],
    )
    diagram.setStyle(
        TableStyle(
            [
                ("GRID", (0, 0), (-1, -1), 0.7, colors.black),
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#EAF1F8")),
                ("PADDING", (0, 0), (-1, -1), 6),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ]
        )
    )
    story.append(diagram)
    story.append(
        p(
            "The <font face='Courier'>displayInfo()</font> method should print the book ID, title, author, and every borrower name on separate lines. Data encapsulation should be preserved.",
            styles["BodyTight"],
        )
    )
    story.append(p("(8 marks)", styles["Small"]))

    story.append(PageBreak())

    story.append(p("15. Implement a class hierarchy for smart home devices using inheritance.", styles["Question"]))
    story.append(
        bullet_list(
            [
                "Create a base class called <font face='Courier'>Device</font> with a function <font face='Courier'>status()</font> that outputs <font face='Courier'>\"Device is operating.\"</font>",
                "Create derived classes <font face='Courier'>Light</font> and <font face='Courier'>AirConditioner</font>.",
                "Override <font face='Courier'>status()</font> in each derived class with a more specific message.",
                "Add a function <font face='Courier'>setBrightness()</font> to <font face='Courier'>Light</font> that outputs the chosen brightness level.",
                "Demonstrate usage in <font face='Courier'>main()</font> by creating one object of each class and calling the relevant functions.",
            ],
            styles["BodyTight"],
        )
    )
    story.append(p("(7 marks)", styles["Small"]))

    story.append(p("16. You are designing a program to model payable staff using runtime polymorphism.", styles["Question"]))
    story.append(
        bullet_list(
            [
                "Create an abstract base class called <font face='Courier'>Employee</font>.",
                "Store the employee name in the base class.",
                "Declare a pure virtual function <font face='Courier'>calculatePay()</font> that returns a <font face='Courier'>double</font>.",
                "Create derived classes <font face='Courier'>FullTimeEmployee</font> and <font face='Courier'>PartTimeEmployee</font>.",
                "Assume a full-time employee stores a fixed monthly salary, while a part-time employee stores hourly wage and hours worked.",
                "Demonstrate usage with base-class pointers, print each employee's pay, and release any dynamic memory properly.",
                "Write constructors and destructors that make the design safe and appropriate.",
            ],
            styles["BodyTight"],
        )
    )
    story.append(p("(10 marks)", styles["Small"]))

    story.append(p("17. Identify and fix the bug(s) or answer the conceptual prompt.", styles["Question"]))
    story.append(p("A. The function is intended to swap two integers.", styles["BodyTight"]))
    story.append(code_block("void swap(int a, int b) {\n    int temp = a;\n    a = b;\n    b = temp;\n}", styles))
    story.append(p("(1 mark)", styles["Small"]))
    story.append(p("B. The function is intended to return the average of all values in the array.", styles["BodyTight"]))
    story.append(code_block("double average(int arr[], int size) {\n    int sum = 0;\n    for (int i = 0; i <= size; i++) {\n        sum += arr[i];\n    }\n    return sum / size;\n}", styles))
    story.append(p("(1 mark)", styles["Small"]))
    story.append(p("C. The following class is intended to manage heap memory safely. Fix the issue.", styles["BodyTight"]))
    story.append(code_block("class Holder {\nprivate:\n    int* data;\npublic:\n    Holder(int value) { data = new int(value); }\n    ~Holder() { }\n};", styles))
    story.append(p("(2 marks)", styles["Small"]))
    story.append(p("D. State the output of the following code.", styles["BodyTight"]))
    story.append(code_block("vector<int> nums = {1, 2, 3, 4, 5, 6};\nauto evenSquare = nums\n    | views::filter([](int x){ return x % 2 == 0; })\n    | views::transform([](int x){ return x * x; });\nfor (int v : evenSquare)\n    cout << v << ' ';", styles))
    story.append(p("(2 marks)", styles["Small"]))
    story.append(p("E. Compare the purpose of <font face='Courier'>const</font> and <font face='Courier'>constexpr</font> with one short example for each.", styles["BodyTight"]))
    story.append(p("(2 marks)", styles["Small"]))

    story.append(Spacer(1, 12))
    story.append(p("- END -", styles["MetaCenter"]))
    return story


def main():
    doc = SimpleDocTemplate(
        OUTPUT_PATH,
        pagesize=A4,
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=16 * mm,
        bottomMargin=20 * mm,
    )
    doc.build(build_story(), onFirstPage=add_header_footer, onLaterPages=add_header_footer)


if __name__ == "__main__":
    main()
