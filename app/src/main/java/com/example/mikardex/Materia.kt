package com.example.mikardex

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cardex")
data class Materia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "materia") val nombre: String,
    val semestre: Int,
    @ColumnInfo(name = "cal_normal") val normal: Double?,
    @ColumnInfo(name = "cal_repeticion") val repeticion: Double?,
    @ColumnInfo(name = "cal_especial") val especial: Double?
) {
    // Regla: Especial > Repetición > Normal
    fun calFinal(): Double? = especial ?: repeticion ?: normal
}