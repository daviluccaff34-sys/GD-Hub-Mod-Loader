# Formato GDHB v1

GDHB é um contêiner binário próprio, simples e sem compressão. Ele guarda arquivos-fonte e manifesto; **não compila** DashCode.

## Estrutura (little-endian)

| Campo | Tamanho | Descrição |
|---|---:|---|
| Magic | 4 bytes | ASCII `GDHB` |
| Version | uint16 | `1` |
| Entry count | uint16 | 1–128 |
| Entry path length | uint16 | 1–240 bytes |
| Entry data length | uint32 | 0–4 MiB |
| Path | variável | UTF-8 válido, relativo, sem `..` |
| Data | variável | bytes do arquivo |

Cada entrada repete os campos de tamanho, caminho e dados. O tamanho total do pacote não pode passar de 16 MiB. Caminhos duplicados, absolutos, com barras invertidas, segmentos vazios ou segmentos `.`/ `..` são rejeitados.

## Arquivos obrigatórios

- `manifest.json`: objeto JSON válido com `id`, `name` e `version` como textos não vazios no nível principal.
- `src/main.dash`: código-fonte principal não vazio em UTF-8 válido.

O leitor valida a estrutura completa do JSON, rejeita chaves duplicadas em cada objeto e não confunde propriedades aninhadas com os campos principais do manifesto. Strings JSON com escapes são aceitas. O leitor ainda não aplica permissões por pacote; permissões continuam sendo uma etapa futura.

## Empacotar

Use `python3 packager/gdhb_pack.py --manifest manifest.json --source main.dash --output meu-mod.gdhb`.

O empacotador apenas reúne os arquivos. O runtime interpreta o código-fonte posteriormente.
