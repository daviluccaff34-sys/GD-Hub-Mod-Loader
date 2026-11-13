# DashCode — linguagem do GD Hub

DashCode é a linguagem simples e interpretada do GD Hub. Arquivos-fonte usam a extensão `.dash`; o empacotador coloca os fontes em um pacote `.gdhb`. **Empacotar não é compilar.**

Este documento separa o que já existe no interpretador do que ainda está planejado.

## 1. Comandos implementados no protótipo

### Mostrar texto

```dash
say "Olá, GD Hub!"
```

### Criar e alterar variáveis

```dash
make nome = "Player"
make vidas = 3
set vidas to 5
add 2 to vidas
say "Jogador: {nome}"
say "Vidas: {vidas}"
```

Tipos atuais: texto, número e booleano (`true` / `false`).

### Condições

```dash
if vidas >= 3
    say "Tudo certo!"
else
    say "Poucas vidas."
end
```

Operadores atuais: `==`, `!=`, `>`, `<`, `>=`, `<=`. Use espaços ao redor dos operadores de comparação.

### Repetição

```dash
repeat 3
    say "Dash!"
end
```

O número deve ser inteiro entre 0 e 1000.

### Comentários

```dash
# Isto é um comentário
say "Olá!"
```

### Interpolação

Coloque o nome de uma variável entre chaves dentro de um texto:

```dash
make fase = "Stereo Madness"
say "Fase atual: {fase}"
```

## 2. Limites de segurança atuais

- Fonte: até 4 MiB.
- Até 100.000 instruções por execução.
- Até 10.000 linhas de saída.
- Cada linha de saída: até 4.096 bytes.
- Cada repetição: inteiro de 0 a 1000.
- Pacotes GDHB: até 16 MiB, com entradas de até 4 MiB.

Esses limites ajudam a evitar que um mod consuma memória ou tempo excessivo.

## 3. Exemplo completo

```dash
make player = "Cube"
make score = 0

repeat 3
    add 10 to score
end

if score >= 30
    say "{player} conseguiu {score} pontos!"
else
    say "Continue tentando."
end
```

## 4. Planejado, ainda não implementado

- Funções e retornos.
- Listas e mapas.
- Operações matemáticas completas.
- Eventos do jogo.
- Interface e botões.
- Triggers e ações do editor.
- Módulos e importações.
- API de Geometry Dash com permissões.

Os 1.200 identificadores do dicionário são vocabulário de referência. Eles não são 1.200 comandos ou funções já implementados.

## 5. Como o pacote funciona

1. O criador escreve os arquivos `.dash`.
2. O empacotador reúne o manifesto e os fontes em um `.gdhb`.
3. O leitor valida o contêiner.
4. O runtime entrega `src/main.dash` ao interpretador.
5. O interpretador executa apenas os recursos disponíveis nesta versão.

Nenhuma ação dentro do Geometry Dash é realizada ainda: a API do jogo depende de uma camada de integração futura.

## Interface gerada pelo DashCode

O runtime também aceita comandos de interface. Eles criam uma camada visual sobre o jogo hospedado pelo GD Hub (não são ainda nós Cocos2d-x nem objetos do editor de fases):

```dash
ui title "Meu primeiro menu"
ui label "Bem-vindo ao meu mod!"
ui button "Continuar"
```

Comandos disponíveis: `ui title valor`, `ui label valor`, `ui button valor` e `ui clear`. O botão atualmente confirma o toque no painel de saída; ações de jogo e objetos de fase ainda dependem da API específica do Geometry Dash.
