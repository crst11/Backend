package co.edu.ucundinamarca.cundiapp.domain.model;

/** Por qué el motor propone una asignatura para el próximo semestre (RF13, SCRUM-244). */
public enum MotivoDeSugerencia {
	/** Le toca en el semestre que sigue según la ruta del plan. */
	SIGUIENTE_SEMESTRE,
	/** Era de un semestre anterior y todavía no la ha aprobado ni la cursa. */
	PENDIENTE,
	/** La reprobó y no la ha vuelto a aprobar. */
	A_REPETIR,
	/** Es de un semestre posterior, pero ya cumple los prerrequisitos y podría adelantarla. */
	ADELANTABLE
}
