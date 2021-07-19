package br.com.zup.chavePix

enum class TiposChavesPix(val tipoChave: String) {

    CPF("cpf"),
    PHONE("telefone"),
    EMAIL("email"),
    RANDOM ("aleatoria"),
    INVALIDA("invalida");

    companion object {
        private val map = TiposChavesPix.values().associateBy(TiposChavesPix::tipoChave)
        fun fromValue(type: String) = map[type]
        fun validaChave(type: String): TiposChavesPix? {
            var teste = "invalida"
            when {
                type.matches("^[0-9]{11}\$".toRegex()) -> {
                    teste = "cpf"
                }
                type.matches("^\\+[1-9][0-9]\\d{1,14}\$".toRegex()) -> {
                    teste = "telefone"
                }
                type.matches("^[a-z0-9.]+@[a-z0-9]+\\.[a-z]+\\.([a-z]+)?\$".toRegex()) -> {
                    teste = "email"
                }
                type.isEmpty() || type.isBlank() -> {
                    teste = "aleatoria"
                }
            }
            return map[teste]
        }
    }
}