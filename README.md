# VideoDownloader

Aplicação web para consultar informações de vídeos do YouTube, ver os formatos e qualidades disponíveis e baixar o arquivo no formato escolhido.

O projeto nasceu como um programa de terminal e foi convertido em uma aplicação web com **Java e Spring Boot** no back-end e **HTML, CSS e JavaScript** na interface. A consulta e o download são feitos pela ferramenta [yt-dlp](https://github.com/yt-dlp/yt-dlp).

> Trabalho acadêmico desenvolvido para a disciplina **[Projeto Integrador: Programação WEB]** — **[Unisa - Universidade Santo Amaro]**.

## Funcionalidades

- Inserir o link de um vídeo e validá-lo (apenas `youtube.com` e `youtu.be`)
- Exibir título, canal e duração
- Listar as qualidades disponíveis de 480p a 4K, com uma opção por resolução
- Escolher uma qualidade, ou a melhor automática (até 4K), e iniciar o download (o áudio é unido ao vídeo automaticamente)
- Acompanhar o andamento em porcentagem, sem travar a página
- Mensagens de andamento, sucesso e erro
- Histórico dos downloads recentes, salvo em banco de dados
- Interface responsiva, com tema claro e escuro

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 17 ou superior | Linguagem do back-end |
| Spring Boot 4 | Servidor web embutido e API REST |
| Maven | Dependências e compilação |
| Spring Data JPA + H2 | Histórico de downloads (banco em arquivo local) |
| Jackson | Leitura do JSON retornado pelo yt-dlp |
| HTML, CSS e JavaScript | Interface (sem frameworks) |
| yt-dlp e ffmpeg | Consulta e download dos vídeos |
| JUnit 5 e Mockito | Testes automatizados |

## Pré-requisitos

Instale e confira que cada comando abaixo funciona no terminal:

| Ferramenta | Como conferir | Observação |
|---|---|---|
| JDK 17 ou superior | `java -version` | |
| Maven 3.9+ | `mvn -version` | Opcional se você abrir o projeto no IntelliJ |
| yt-dlp | `yt-dlp --version` | |
| ffmpeg | `ffmpeg -version` | Une vídeo e áudio nos formatos de alta qualidade |

### Instalando yt-dlp e ffmpeg

**Windows** (PowerShell):

```powershell
winget install yt-dlp.yt-dlp
winget install Gyan.FFmpeg
```

Feche e abra o terminal depois de instalar.

**macOS** (Homebrew):

```bash
brew install yt-dlp ffmpeg
```

**Linux** (exemplo Debian/Ubuntu):

```bash
sudo apt install ffmpeg
python3 -m pip install -U yt-dlp   # ou baixe o binário em github.com/yt-dlp/yt-dlp
```

## Como executar

1. Clone o repositório:

   ```bash
   git clone https://github.com/victor-ferrarezi/VideoDownloader.git
   cd VideoDownloader
   ```

2. Confira a configuração em `src/main/resources/application.properties` (veja a seção abaixo). Por padrão o comando é `yt-dlp`.

3. Inicie a aplicação:

   ```bash
   mvn spring-boot:run
   ```

   Ou, no IntelliJ IDEA, execute a classe `VideoDownloaderApplication`.

4. Abra no navegador: **http://localhost:8080**


## Como usar

1. Com a aplicação em execução, abra http://localhost:8080.
2. Cole o link de um vídeo do YouTube no campo e clique em **Consultar vídeo**.
3. Confira o título, o canal e a duração, e escolha a qualidade na lista (ou deixe "Melhor qualidade").
4. Clique em **Baixar vídeo** e acompanhe a barra de progresso.
5. Ao terminar, o navegador salva o arquivo. Os downloads recentes aparecem em "Downloads recentes".

Se aparecer uma mensagem de erro em vermelho, ela explica o problema (link inválido, vídeo indisponível, servidor desligado, entre outros).

## Configuração

Arquivo `src/main/resources/application.properties`:

| Propriedade | Padrão | Descrição |
|---|---|---|
| `app.ytdlp.command` | `yt-dlp` | Comando para chamar o yt-dlp, separado por vírgula. Ex.: `py,-m,yt_dlp` ou `python,-m,yt_dlp` |
| `app.ytdlp.downloads-dir` | `Downloads` | Pasta onde os downloads são salvos |
| `server.port` | `8080` | Porta do servidor (acrescente a linha para mudar) |

Nenhuma senha ou token é necessário. O banco H2 usa um arquivo local (`./data/historico.mv.db`), que não vai para o repositório.

## API REST

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/videos/info` | Título, canal e duração |
| POST | `/api/videos/formats` | Lista de formatos |
| POST | `/api/downloads` | Inicia um download em segundo plano (resposta 202 com o `id`) |
| GET | `/api/downloads/{id}` | Situação e porcentagem |
| GET | `/api/downloads/{id}/file` | Baixa o arquivo, quando concluído |
| GET | `/api/historico` | Downloads recentes |

Exemplo:

```bash
curl -X POST http://localhost:8080/api/videos/info \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.youtube.com/watch?v=jNQXAC9IJRE"}'
```

Os erros retornam JSON no formato `{ "status": 400, "mensagem": "..." }`.

## Estrutura do projeto

```
src/main/java/com/videodownloader/
├── VideoDownloaderApplication.java   ponto de entrada
├── config/        leitura das configurações
├── controller/    recebe as requisições HTTP
├── dto/           formatos de entrada e saída da API
├── exception/     erros da API e tratamento global
├── model/         classes de domínio e entidade do banco
├── repository/    acesso ao banco de dados
├── service/       regras de negócio (yt-dlp, downloads, histórico)
└── validator/     validação de URLs

src/main/resources/
├── application.properties
└── static/        interface (index.html, style.css, app.js)

src/test/java/     testes automatizados
docs/              coleção do Postman para testar a API
```

Fluxo: **controller → service → yt-dlp / repository → banco de dados**. Cada camada conversa apenas com a de baixo.

## Testes

Testes automatizados (JUnit 5 e Mockito):

```bash
mvn test
```

Testes manuais da API: importe `docs/VideoDownloader.postman_collection.json` no Postman (cada requisição traz o resultado esperado e testes automáticos), ou use o arquivo `requests.http` no IntelliJ.

Os resultados dos testes (esperado × obtido) estão no relatório do trabalho.

## Limitações conhecidas

- Funciona apenas com links do YouTube.
- Exige yt-dlp e ffmpeg instalados na máquina que executa o servidor.
- O andamento dos downloads fica em memória e é perdido ao reiniciar o servidor (o histórico permanece no banco).
- Não há autenticação nem limpeza automática da pasta de downloads.

## Aviso legal

Projeto de fins educacionais e de uso pessoal. Respeite os termos de uso das plataformas e os direitos autorais do conteúdo. Não hospede este serviço publicamente.

## Autores

**[Victor de Sousa Ferrarezi]** — **[Lucas Siqueira]** — **[Fenniguy Tallys Barros]** — [Analise e Desenvolvimento de Sistemas], [Unisa - Universidade Santo Amaro]