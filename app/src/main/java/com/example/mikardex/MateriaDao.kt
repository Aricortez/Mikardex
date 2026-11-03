package com.example.mikardex

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MateriaDao {
    @Insert
    fun insertar(materia: Materia)

    @Update
    fun actualizar(materia: Materia)

    @Delete
    fun eliminar(materia: Materia)

    @Query("SELECT * FROM cardex ORDER BY semestre, materia")
    fun obtenerTodas(): List<Materia>

    @Query("SELECT AVG(COALESCE(cal_especial, cal_repeticion, cal_normal)) FROM cardex")
    fun promedioGeneral(): Double?

    @Query("SELECT AVG(COALESCE(cal_especial, cal_repeticion, cal_normal)) FROM cardex WHERE semestre = :sem")
    fun promedioSemestre(sem: Int): Double?
}