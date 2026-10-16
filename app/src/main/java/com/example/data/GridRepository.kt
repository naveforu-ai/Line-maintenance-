package com.example.data

import kotlinx.coroutines.flow.Flow

class GridRepository(private val db: AppDatabase) {
    val allTasks: Flow<List<TaskEntity>> = db.taskDao.getAllTasks()
    val allDefects: Flow<List<DefectEntity>> = db.defectDao.getAllDefects()

    suspend fun checkAndSeedInitialTasks() {
        val count = db.taskDao.getTaskCount()
        if (count == 0) {
            val defaults = listOf(
                TaskEntity(
                    id = "JOB-AP-VJA-12",
                    title = "Emergency Jungle Clearance Segment 4",
                    locationFrom = "AP-VJA-04",
                    locationTo = "AP-VJA-06",
                    voltage = "220kV Feeder",
                    lineName = "Nellore - Vijayawada Ckt-1",
                    type = "EJC",
                    reportTime = "08:30 AM",
                    numMembersOtherWorks = 2,
                    otherWorksDetails = "Tidying ground yard pits.",
                    assignedBy = "C. Rama Rao (Control)",
                    status = "AWAITING_PTW",
                    priority = "CRITICAL"
                ),
                TaskEntity(
                    id = "JOB-AP-NLR-02",
                    title = "Replace Flash Insulator strings segment",
                    locationFrom = "AP-NLR-01",
                    locationTo = "AP-NLR-03",
                    voltage = "132kV Segment",
                    lineName = "Vijayawada - Nellore Ckt-2",
                    type = "Insulator replacement",
                    reportTime = "08:00 AM",
                    numMembersOtherWorks = 0,
                    otherWorksDetails = "",
                    assignedBy = "V. Krishna (AEE)",
                    status = "AWAITING_PTW",
                    priority = "HIGH"
                ),
                TaskEntity(
                    id = "JOB-AP-VZG-14",
                    title = "Line clearance vegetation cut downs",
                    locationFrom = "AP-VZG-12",
                    locationTo = "AP-VZG-14",
                    voltage = "400kV Segment",
                    lineName = "Vizag - Nellore Ckt-1",
                    type = "JC",
                    reportTime = "07:30 AM",
                    numMembersOtherWorks = 1,
                    otherWorksDetails = "Inventory ledger update",
                    assignedBy = "Srinivas Rao (DEE)",
                    status = "COMPLETED",
                    priority = "NORMAL"
                )
            )
            db.taskDao.insertTasks(defaults)
        }
    }

    suspend fun updateTaskStatus(id: String, status: String) {
        db.taskDao.updateTaskStatus(id, status)
    }

    suspend fun insertDefect(defect: DefectEntity) {
        db.defectDao.insertDefect(defect)
    }

    suspend fun deleteDefect(localId: Int) {
        db.defectDao.deleteDefect(localId)
    }
}
