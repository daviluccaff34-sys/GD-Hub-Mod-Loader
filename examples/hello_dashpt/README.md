# Hello DashPT

Exemplo inicial de um mod escrito em DashPT e compilado para o DashCode reconhecido pelo runtime atual.

- `src/main.dashpt`: código de autoria em português.
- `src/main.dash`: saída compilada que o runtime atual interpreta.
- `manifest.json`: manifesto GDHB.

Empacote com:

```sh
python3 packager/gdhb_pack.py --manifest examples/hello_dashpt/manifest.json --source examples/hello_dashpt/src/main.dash --output hello-dashpt.gdhb
```

Este exemplo demonstra execução do interpretador (mensagens e variáveis). Não altera objetos, triggers ou gameplay de Geometry Dash.
