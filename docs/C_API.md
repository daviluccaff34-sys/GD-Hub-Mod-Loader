# GD Hub C API v1

A C API versionada oferece uma fronteira simples para hosts nativos futuros. Ela permite criar um runtime, executar um pacote GDHB e receber a saída em texto sem depender das classes C++.

Header: `runtime/c_api/gdhub_c_api.h`

- `gdhub_api_version()`: retorna a versão 1.
- `gdhub_runtime_create()`: cria um handle ou retorna null se a alocação falhar.
- `gdhub_runtime_run_package(...)`: valida e executa um pacote.
- `gdhub_runtime_last_error(...)`: lê o último erro do handle.
- `gdhub_runtime_destroy(...)`: libera o handle; aceitar null é seguro.

## Códigos de retorno

- `0`: sucesso.
- `-1`: argumentos inválidos.
- `-2`: erro de pacote ou execução DashCode.
- `-3`: buffer de saída pequeno; `output_required` informa os bytes necessários, incluindo o NUL final.

## Uso e limites

O host deve chamar funções de um mesmo handle sequencialmente. Handles diferentes podem ser usados independentemente. O ponteiro retornado por `gdhub_runtime_last_error` pertence ao handle e não deve ser guardado depois de destruir o handle ou iniciar outra chamada que altere seu erro.

Esta API ainda não está conectada ao Geometry Dash. É apenas a fronteira do runtime que uma futura integração verificada poderá chamar.
