package od.cli

import od.util.Dice
import od.util.RaceAbilityNames
import od.core.*
import od.io.CharacterIO
import java.io.File

// printo a string ate receber um valor valido, (Nao vazio), prevento o spam de "enter"
private fun readNonEmpty(prompt:String): String {
    while(true){
        print(prompt)
        val s = readLine()?.trim() ?: ""
        // se nao vazio retorno a string
        if(s.isNotEmpty()){
            return s
        }

        println("Entrada vazia.")
    }
}

// outro metodo de verificacao de input, printo a string prompt ate vir uma valida dentro de um range
private fun chooseInt(prompt:String, range:IntRange): Int {
    while(true){
        print(prompt)
        val v = readLine()?.trim()?.toIntOrNull()
        if(v!=null && v in range) return v
        println("Escolha um número em $range.")
    }
}

// caso usuario use o modo classico, basicamente copio o que esta escrito no livor
private fun rollClassic(): Attributes {
    // Clássico: 3d6 em ordem.  FOR, DES, CON, INT, SAB, CAR.
    val vals = IntArray(6){ Dice.roll3d6() }
    return Attributes(vals[0],vals[1],vals[2],vals[3],vals[4],vals[5])
}

// rolada normal
private fun rollPool3d6(): MutableList<Int> = MutableList(6){
    // metodos declarados na objeto DADO
    Dice.roll3d6()
}

// Heroico: 4d6 drop lowest, seis vezes. mesma coisa que o passado
private fun rollPool4d6Drop(): MutableList<Int> = MutableList(6){
    Dice.roll4d6DropLowest()
}

// esse metodo apenas aplica as roladas
private fun assignPoolToAttributes(pool:MutableList<Int>): Attributes {
    // atributos a serem preenchidos pelas roladas
    val A = Attributes(0,0,0,0,0,0)
    // mostro a pool de possiveis valores
    println("Rolagens: ${pool.joinToString()}")
    // atribuo dentro dos atributos para cada idx valido
    Attributes.names.forEachIndexed { idx, name ->
        while(true){
            // mostro os valores livres
            println("Atribuir para $name um dos valores ainda livres: ${pool.joinToString()}")
            print("> ")
            // leio do stdin, e converto para int apenas o numero
            val v = readLine()?.trim()?.toIntOrNull()
            // verifico se e valido na lista
            if(v!=null && pool.remove(v)){
                // caso seja eu atribuo
                A[idx] = v
                break
                // caso nao seja eu continuo o loop
            } else println("Valor inválido, tente novamente.")
        }
    }
    return A
}

// metodo simples para o player escolher sua raca
private fun chooseRace(): Race {
    // mostro cada raca criada junto com suas abilidades
    println("Raças:")
    Race.all.forEachIndexed { i, r ->
        val infr = r.infravisao?.let{"${it}m"} ?: "não"
        println("${i+1}) ${r.nome}  MV=${r.movimento}m  Infravisão=$infr  Alinhamento=${r.alinhamento}")
    }
    //
    val i = chooseInt("Escolha a raça [1-${Race.all.size}]: ", 1..Race.all.size) - 1
    return Race.all[i]
}

// simples metodo para escolher a classe:
// guerreiro
// clerigo
// mago
private fun chooseClass(): CharacterClass {
    println("Classes:")
    // para cada classe
    CharacterClass.all.forEachIndexed { i, c -> println("${i+1}) ${c.nome}") }
    val i = chooseInt("Escolha a classe [1-${CharacterClass.all.size}]: ", 1..CharacterClass.all.size) - 1
    // retorno a classes escolhida
    return CharacterClass.all[i]
}

// caso humano, falta o JP
private fun finalizeCharacter(nome:String, A:Attributes, r:Race, c:CharacterClass): Character {
    val jpHuman = if(r is Race.Humano){
        println("Humano: escolha uma JP para +1 (texto livre, ex.: 'JPS', 'JPC', etc.):")
        print("> "); readLine()?.trim().takeUnless { it.isNullOrEmpty() }
    } else null
    // retorno o personagem
    return Character(nome, A, r, c, jpHuman, r.habilidadesMask)
}

// printo os atributos junto com algumas informacoes
private fun printSummary(ch: Character){
    println("\n=== Personagem ===")
    println("Nome: ${ch.nome}")
    println("Classe: ${ch.classe.nome}")
    println("Raça: ${ch.raca.nome} | MV=${ch.raca.movimento}m | Infravisão=${ch.raca.infravisao ?: "não"} | Alinhamento=${ch.raca.alinhamento}")
    println("Atributos: ${ch.atributos}")
    val abil = RaceAbilityNames.decode(ch.habilidadesRaciaisMask)
    println("Habilidades raciais: ${if(abil.isEmpty()) "(nenhuma)" else abil.joinToString()}")
    if(ch.humanoJPEscolhida!=null) println("JP escolhida (Humano): ${ch.humanoJPEscolhida}")
}

// funcao principal
fun main(){
    println("=== Old Dragon 2 - Criador de Personagem (CLI) {PEDRO DE OLIVEIRA HARO} ===")
    val nome = readNonEmpty("Nome do personagem: ")

    println("\nEstilo de atributos:")
    println("1) Clássico (3d6 em ordem)")
    println("2) Aventureiro (3d6; distribua valores)")
    println("3) Heroico (4d6 descarta menor; distribua valores)")
    val estilo = chooseInt("Escolha [1-3]: ", 1..3)

    val A = when(estilo){
        1 -> rollClassic()
        2 -> assignPoolToAttributes(rollPool3d6())
        else -> assignPoolToAttributes(rollPool4d6Drop())
    }
    println("Atributos gerados: $A")

    val r = chooseRace()
    val c = chooseClass()
    val ch = finalizeCharacter(nome, A, r, c)
    printSummary(ch)

    val dir = File("characters")
    dir.mkdirs()
    val path = "characters/${nome.replace(Regex("""\s+"""), "_")}.json"
    CharacterIO.saveJson(ch, path)
    println("\nSalvo em: $path")
}
