# Projeto SD

Sistema cliente-servidor em Java para comunicação concorrente.

---

## 🗂️ Estrutura do Repositório

* **`/src/Servidor`**: Código do servidor (gestor central, concorrência e *workers*).
* **`/src/Client`**: Código do cliente (interface e *demultiplexer* de mensagens).
* **`/src/Main`**: Classes partilhadas e utilitários (conectores, *frames* e eventos).

---

## 🚀 Como Executar o Programa

Este é um projeto em **Java** nativo. Certifica-te de que tens o Java Development Kit (JDK) instalado.

Abre o teu terminal, navega até à pasta **`src`** do repositório e segue os passos:

### 1️⃣ Compilar o código
Antes de executar, compila todos os ficheiros Java com o seguinte comando:
```bash
javac Client/*.java Servidor/*.java Main/*.java

2️⃣ Iniciar o servidor

Uma vez compilado, inicia primeiro o processo do servidor para este ficar à escuta de ligações:
Bash

java Servidor.ServidorMain

3️⃣ Iniciar o cliente

Abre um novo terminal (deixando o servidor a correr), navega novamente para a pasta src e inicia o cliente:
Bash

java Client.ClienteMain
