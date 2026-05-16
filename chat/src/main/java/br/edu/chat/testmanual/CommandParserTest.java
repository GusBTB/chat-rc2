package br.edu.chat.testmanual;

import br.edu.chat.protocol.ClientCommand;
import br.edu.chat.protocol.CommandParser;
import br.edu.chat.protocol.CommandType;

public class CommandParserTest {

    private static int total;
    private static int falhas;

    public static void main(String[] args) {
        CommandParser parser = new CommandParser();

        check(parser.parse("cadastro;Joao Silva;joao;joao@email.com;123"),
                CommandType.CADASTRO,
                "cadastro valido");

        check(parser.parse("cadastro;Joao;joao;joao@email.com"),
                CommandType.INVALID,
                "cadastro com numero errado de campos");

        check(parser.parse("login;joao;123"),
                CommandType.LOGIN,
                "login valido");

        check(parser.parse("login;;senha"),
                CommandType.INVALID,
                "login com login vazio");

        check(parser.parse("logout"),
                CommandType.LOGOUT,
                "logout");

        check(parser.parse("recuperarsenha;joao@email.com"),
                CommandType.RECUPERAR_SENHA,
                "recuperar senha");

        check(parser.parse("status;ONLINE"),
                CommandType.STATUS,
                "status online");

        check(parser.parse("status;qualquercoisa"),
                CommandType.INVALID,
                "status invalido");

        check(parser.parse("listausuarios"),
                CommandType.LISTA_USUARIOS,
                "listar usuarios");

        check(parser.parse("listagrupos"),
                CommandType.LISTA_GRUPOS,
                "listar grupos");

        check(parser.parse("listargrupo &amigos"),
                CommandType.LISTAR_GRUPO,
                "listar grupo amigos");

        check(parser.parse("listargrupo"),
                CommandType.INVALID,
                "listargrupo sem nome");

        ClientCommand direct = parser.parse("@robson: como vc vai?");
        check(direct, CommandType.DIRECT_MESSAGE, "msg direta");
        assertEqual("robson", direct.getTargetUsers().get(0), "destinatario msg direta");
        assertEqual("como vc vai?", direct.getContent(), "conteudo msg direta");

        check(parser.parse("@robson"),
                CommandType.INVALID,
                "msg direta sem :");

        check(parser.parse("novogrupo amigos"),
                CommandType.NOVO_GRUPO,
                "novo grupo amigos");

        ClientCommand inserir = parser.parse("inserir &amigos@robson,@siscoutto");
        check(inserir, CommandType.INSERIR_GRUPO, "inserir multi usuarios");
        assertEqual("amigos", inserir.getGroupName(), "nome do grupo no inserir");
        assertEqual(2, inserir.getTargetUsers().size(), "qtd usuarios no inserir");

        ClientCommand inserir2 = parser.parse("inserir &amigos@robson");
        check(inserir2, CommandType.INSERIR_GRUPO, "inserir um usuario");
        assertEqual(1, inserir2.getTargetUsers().size(), "qtd usuarios no inserir2");

        check(parser.parse("entrar &amigos"),
                CommandType.ENTRAR_GRUPO,
                "entrar em grupo");

        check(parser.parse("sair &amigos"),
                CommandType.SAIR_GRUPO,
                "sair de grupo");

        check(parser.parse("promover &amigos@robson"),
                CommandType.PROMOVER_ADMIN,
                "promover admin");

        ClientCommand grupoMsg = parser.parse("&amigos: ola pessoal");
        check(grupoMsg, CommandType.GROUP_MESSAGE, "msg para grupo");
        assertEqual("amigos", grupoMsg.getGroupName(), "nome grupo");
        assertEqual("ola pessoal", grupoMsg.getContent(), "conteudo grupo");

        ClientCommand grupoDireta = parser.parse("&amigos@robson: ola robson");
        check(grupoDireta, CommandType.GROUP_DIRECT_MESSAGE, "msg grupo direta");
        assertEqual("amigos", grupoDireta.getGroupName(), "nome grupo direta");
        assertEqual(1, grupoDireta.getTargetUsers().size(), "qtd destinatarios direta");

        ClientCommand grupoDiretaMulti = parser.parse("&amigos@robson,@siscoutto: ola galera");
        check(grupoDiretaMulti, CommandType.GROUP_DIRECT_MESSAGE, "msg grupo direta multi");
        assertEqual(2, grupoDiretaMulti.getTargetUsers().size(), "qtd destinatarios direta multi");

        check(parser.parse("&amigos:"),
                CommandType.INVALID,
                "msg grupo vazia");

        check(parser.parse("bloquear @robson"),
                CommandType.BLOQUEAR,
                "bloquear usuario");

        check(parser.parse("desbloquear @robson"),
                CommandType.DESBLOQUEAR,
                "desbloquear usuario");

        ClientCommand aceitar = parser.parse("aceitar 15");
        check(aceitar, CommandType.ACEITAR, "aceitar pedido");
        assertEqual(15, aceitar.getRequestId(), "id do pedido");

        check(parser.parse("recusar 20"),
                CommandType.RECUSAR,
                "recusar pedido");

        check(parser.parse("aceitar abc"),
                CommandType.INVALID,
                "aceitar com id nao numerico");

        check(parser.parse("comandoinexistente xpto"),
                CommandType.UNKNOWN,
                "comando desconhecido");

        check(parser.parse(""),
                CommandType.INVALID,
                "comando vazio");

        check(parser.parse(null),
                CommandType.INVALID,
                "comando null");

        check(parser.parse("ajuda"),
                CommandType.AJUDA,
                "ajuda");

        System.out.println();
        System.out.println("==============================================");
        System.out.println("Total: " + total + " | Falhas: " + falhas);
        if (falhas == 0) {
            System.out.println("Todos os testes passaram.");
        } else {
            System.out.println("Existem falhas. Verifique os logs acima.");
        }
    }

    private static void check(ClientCommand cmd, CommandType expected, String label) {
        total++;
        if (cmd.getType() == expected) {
            System.out.println("OK   - " + label + " -> " + cmd.getType());
        } else {
            falhas++;
            System.out.println("FALHA- " + label
                    + " esperado=" + expected
                    + " obtido=" + cmd.getType()
                    + " erro=" + cmd.getErrorMessage());
        }
    }

    private static void assertEqual(Object expected, Object actual, String label) {
        total++;
        boolean ok = (expected == null && actual == null)
                || (expected != null && expected.equals(actual));
        if (ok) {
            System.out.println("OK   - " + label + " -> " + actual);
        } else {
            falhas++;
            System.out.println("FALHA- " + label
                    + " esperado=" + expected
                    + " obtido=" + actual);
        }
    }
}
