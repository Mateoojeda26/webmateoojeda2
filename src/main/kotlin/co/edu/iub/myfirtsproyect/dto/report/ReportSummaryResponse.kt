package co.edu.iub.myfirtsproyect.dto.report

data class PetReportRow(
    val petId: Long,
    val petName: String,
    val total: Int,
    val pending: Int,
    val overdue: Int,
    val completed: Int,
    val skipped: Int,
    val complianceRate: Int,
)

data class ReportSummaryResponse(
    val total: Int,
    val pending: Int,
    val overdue: Int,
    val completed: Int,
    val skipped: Int,
    val complianceRate: Int,
    val perPet: List<PetReportRow>,
)
