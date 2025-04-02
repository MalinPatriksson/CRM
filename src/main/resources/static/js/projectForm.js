document.addEventListener("DOMContentLoaded", function () {
    // Hantera att lägga till rader
    document.getElementById("addRowBtn").addEventListener("click", addRow);

    // Hantera klick på soptunnan för att ta bort rader
    document.getElementById("budgetTableBody").addEventListener("click", function (event) {
        if (event.target.closest(".remove-row")) {
            let row = event.target.closest("tr");
            let idInput = row.querySelector("input[name*='id']");

            if (idInput && idInput.value) {
                let deletedInput = document.createElement("input");
                deletedInput.type = "hidden";
                deletedInput.name = "deletedBudgetRows";
                deletedInput.value = idInput.value;
                document.querySelector("form").appendChild(deletedInput);
            }

            row.remove();
        }
    });

    // Hantera formulärinsändning
    document.getElementById("projectForm").addEventListener("submit", submitForm);

    // Hantera PDF-uppladdning och förhandsvisning
    setupPdfHandling();
});

function addRow() {
    let tableBody = document.getElementById("budgetTableBody");
    let newRow = document.createElement("tr");

    let columnCount = tableBody.rows[0].cells.length;

    newRow.innerHTML = `
        <td><input type="text" name="budgetRows[new][Rubrik]" class="form-control"/></td>
        ${[...Array(columnCount - 3)].map(() => `<td><input type="text" class="form-control" name="budgetRows[new][]"/></td>`).join('')}
        <td><input type="text" name="budgetRows[new][Total]" class="form-control"/></td>
        <td>
            <button type="button" class="btn btn-danger btn-sm remove-row">
                <i class="bi bi-trash-fill"></i>
            </button>
            <input type="hidden" name="budgetRows[new][id]" value=""/>
        </td>
    `;

    tableBody.appendChild(newRow);
}

function submitForm(event) {
    // Validering kan läggas till här om nödvändigt
    console.log("📤 Formuläret skickas...");
}

// Hantera PDF-uppladdning och förhandsvisning
function setupPdfHandling() {
    const fileInput = document.getElementById("file");
    const pdfViewer = document.getElementById("pdfViewer");
    const pdfButton = document.getElementById("viewPdfButton");

    // Hämta tidigare sparad PDF från backend
    fetch("/get-latest-pdf")
        .then(response => response.json())
        .then(data => {
            if (data.pdfUrl) {
                pdfViewer.src = data.pdfUrl;
                pdfViewer.style.display = "block";
                pdfButton.style.display = "inline-block";
                sessionStorage.setItem("uploadedPdf", data.pdfUrl); // 🟢 Behåller senaste PDF
            }
        })
        .catch(error => console.error("Misslyckades att ladda PDF:", error));

    // När användaren laddar upp en ny PDF
    fileInput.addEventListener("change", function (event) {
        const file = event.target.files[0];
        if (file) {
            const fileURL = URL.createObjectURL(file);
            pdfViewer.src = fileURL;
            pdfViewer.style.display = "block";
            pdfButton.style.display = "inline-block";
            sessionStorage.setItem("uploadedPdf", fileURL); // 🟢 Sparar filen temporärt i sessionStorage
        }
    });
}


