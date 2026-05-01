package com.example.guardwayapplication

/**
 * Classe de domínio responsável pela validação e integridade de dados do usuário.
 * RENOMEADA de "Usuario" para evitar conflito com a data class Usuario.kt existente.
 */
class UsuarioValidador {

    private val REGEX_EMAIL =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(\\.[A-Za-z]{2,})?$")

    private val REGEX_SENHA_NUMERO   = Regex(".*[0-9].*")
    private val REGEX_SENHA_ESPECIAL = Regex(".*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")
    private val REGEX_NOME           = Regex("^[A-Za-zÀ-ÿ ]{3,60}$")

    fun validarEmail(email: String): Boolean {
        if (email.isBlank()) return false
        return REGEX_EMAIL.matches(email.trim())
    }

    fun validarSenha(senha: String): Boolean {
        if (senha.length < 8) return false
        if (!REGEX_SENHA_NUMERO.matches(senha)) return false
        if (!REGEX_SENHA_ESPECIAL.matches(senha)) return false
        return true
    }

    fun validarNome(nome: String): Boolean {
        if (nome.isBlank()) return false
        return REGEX_NOME.matches(nome.trim())
    }

    fun aplicarMascaraTelefone(input: String): String? {
        val digits = input.filter { it.isDigit() }
        if (digits.length != 11) return null
        val ddd    = digits.substring(0, 2)
        val parte1 = digits.substring(2, 7)
        val parte2 = digits.substring(7, 11)
        return "($ddd) $parte1-$parte2"
    }

    fun validarTelefone(telefone: String): Boolean {
        val REGEX_FONE = Regex("^\\(\\d{2}\\) \\d{5}-\\d{4}$")
        return REGEX_FONE.matches(telefone)
    }

    fun validarCadastroCompleto(
        nome: String, email: String, senha: String, telefone: String
    ): List<String> {
        val erros = mutableListOf<String>()
        if (!validarNome(nome))     erros.add("Nome inválido: use entre 3 e 60 letras ou espaços.")
        if (!validarEmail(email))   erros.add("E-mail inválido: informe um e-mail no formato usuario@dominio.com.")
        if (!validarSenha(senha))   erros.add("Senha inválida: mínimo 8 caracteres, com número e caractere especial.")
        if (!validarTelefone(telefone)) erros.add("Telefone inválido: use o formato (XX) XXXXX-XXXX.")
        return erros
    }
}