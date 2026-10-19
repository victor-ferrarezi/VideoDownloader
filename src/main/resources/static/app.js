'use strict';   // modo estrito: o navegador avisa de erros comuns de digitação

/* ==========================================================
   app.js - o "comportamento" da página.

   Este arquivo faz só duas coisas:
     1. chama a API do servidor (fetch);
     2. mostra ou esconde partes da página conforme a resposta.

   Nenhuma regra de negócio mora aqui (por exemplo, quem decide se
   um link é válido é o servidor). Assim, um futuro app de celular
   ou de PC reaproveita a mesma API sem reescrever regras.
   ========================================================== */


/* ---------- 1. Atalhos para os elementos da página ----------
   document.getElementById('x') acha o elemento que no HTML tem id="x".
   Guardamos tudo num objeto para não repetir a busca. */
const $ = (id) => document.getElementById(id);

const el = {
    form: $('form-url'),
    campoUrl: $('campo-url'),
    btnConsultar: $('btn-consultar'),
    mensagem: $('mensagem'),

    painelInfo: $('painel-info'),
    infoTitulo: $('info-titulo'),
    infoCanal: $('info-canal'),
    infoDuracao: $('info-duracao'),

    painelFormatos: $('painel-formatos'),
    listaFormatos: $('lista-formatos'),
    btnBaixar: $('btn-baixar'),

    painelProgresso: $('painel-progresso'),
    percentual: $('percentual'),
    barra: $('barra'),
    barraPreenchida: $('barra-preenchida'),
    progressoTexto: $('progresso-texto'),

    painelHistorico: $('painel-historico'),
    listaHistorico: $('lista-historico'),
    historicoVazio: $('historico-vazio'),
};

// Variáveis que a página "lembra" enquanto está aberta
let urlAtual = null;       // o último link consultado com sucesso
let temporizador = null;   // controla a consulta periódica do andamento


/* ---------- 2. Comunicação com a API ---------- */

// Chama o servidor e devolve o JSON da resposta.
// Se algo der errado, lança um Error com uma mensagem legível.
//
// "async/await": fetch demora (é rede). O "await" faz a função esperar
// a resposta sem travar a página.
async function api(caminho, opcoes) {
    let resposta;

    try {
        resposta = await fetch(caminho, opcoes);
    } catch (e) {
        // fetch só lança erro quando NEM CHEGOU ao servidor
        // (servidor desligado, sem rede...)
        throw new Error('Não foi possível se comunicar com o servidor.');
    }

    let dados = null;
    try {
        dados = await resposta.json();
    } catch (e) {
        // resposta sem corpo JSON: segue com dados = null
    }

    // resposta.ok é falso para códigos 400, 404, 500...
    if (!resposta.ok) {
        // nossa API manda { status, mensagem } nos erros
        throw new Error(dados?.mensagem || `Erro inesperado (${resposta.status}).`);
    }

    return dados;
}

// Monta as opções de uma requisição POST com corpo JSON
function postJson(objeto) {
    return {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(objeto),   // transforma o objeto em texto JSON
    };
}


/* ---------- 3. Mensagens e visibilidade ---------- */

// tipo: 'info' (azul), 'sucesso' (verde) ou 'erro' (vermelho)
function mostrarMensagem(tipo, texto) {
    el.mensagem.className = `mensagem ${tipo}`;
    el.mensagem.textContent = texto;   // textContent: nunca interpreta HTML
    el.mensagem.hidden = false;
}

function esconderResultados() {
    el.painelInfo.hidden = true;
    el.painelFormatos.hidden = true;
    el.painelProgresso.hidden = true;
}

function pararAcompanhamento() {
    clearTimeout(temporizador);
    temporizador = null;
}


/* ---------- 4. Consultar o vídeo ---------- */

// 'submit' acontece quando o usuário clica em "Consultar" ou aperta Enter
el.form.addEventListener('submit', async (evento) => {
    evento.preventDefault();   // impede o navegador de recarregar a página

    const url = el.campoUrl.value.trim();   // trim() tira espaços das pontas

    if (!url) {
        mostrarMensagem('erro', 'Cole o link de um vídeo para continuar.');
        return;
    }

    pararAcompanhamento();
    esconderResultados();
    el.btnConsultar.disabled = true;   // evita cliques duplos
    mostrarMensagem('info', 'Consultando o vídeo. Isso pode levar alguns segundos.');

    try {
        // Promise.all faz as duas consultas ao mesmo tempo
        // e espera as duas terminarem.
        const [info, formatos] = await Promise.all([
            api('/api/videos/info', postJson({ url })),
            api('/api/videos/formats', postJson({ url })),
        ]);

        urlAtual = url;
        mostrarInfo(info);
        mostrarFormatos(formatos);
        mostrarMensagem('sucesso', 'Vídeo encontrado. Escolha a qualidade e clique em Baixar vídeo.');

    } catch (erro) {
        mostrarMensagem('erro', erro.message);

    } finally {
        // finally roda sempre, com sucesso ou erro
        el.btnConsultar.disabled = false;
    }
});

function mostrarInfo(info) {
    el.infoTitulo.textContent = info.titulo;
    el.infoCanal.textContent = info.canal;
    el.infoDuracao.textContent = info.duracaoFormatada;
    el.painelInfo.hidden = false;
}


/* ---------- 5. Lista de formatos ---------- */

// "1920x1080" vira 1080 (usado para ordenar do maior para o menor)
function alturaDe(formato) {
    const m = /x(\d+)/.exec(formato.resolucao);
    return m ? Number(m[1]) : 0;
}

// Cria uma linha da lista: um botão de rádio + o texto da opção
function criarOpcao({ valor, temAudio, titulo, detalhe, marcado }) {
    const rotulo = document.createElement('label');
    rotulo.className = 'opcao';

    const radio = document.createElement('input');
    radio.type = 'radio';
    radio.name = 'formato';
    radio.value = valor;
    radio.dataset.temAudio = String(temAudio);   // guarda um dado extra no elemento
    radio.checked = marcado;

    const texto = document.createElement('span');
    texto.className = 'opcao-texto';

    const forte = document.createElement('strong');
    forte.textContent = titulo;

    const pequeno = document.createElement('small');
    pequeno.textContent = detalhe;

    texto.append(forte, pequeno);
    rotulo.append(radio, texto);
    return rotulo;
}

function mostrarFormatos(formatos) {
    el.listaFormatos.replaceChildren();   // limpa a lista anterior

    // Primeira opção: o servidor escolhe a melhor qualidade
    el.listaFormatos.append(criarOpcao({
        valor: '',
        temAudio: false,
        titulo: 'Melhor qualidade (automático)',
        detalhe: 'Vídeo e áudio nas melhores qualidades disponíveis',
        marcado: true,
    }));

    // [...formatos] copia a lista para não alterar a original ao ordenar
    [...formatos]
        .sort((a, b) => alturaDe(b) - alturaDe(a))   // maior resolução primeiro
        .forEach((f) => {
            const partes = [f.extensao];
            if (f.descricao) partes.push(f.descricao);
            partes.push(f.temAudio ? 'com áudio' : 'sem áudio (será juntado ao melhor áudio)');
            partes.push(`id ${f.id}`);

            el.listaFormatos.append(criarOpcao({
                valor: f.id,
                temAudio: f.temAudio,
                titulo: f.resolucao,
                detalhe: partes.join(', '),
                marcado: false,
            }));
        });

    el.painelFormatos.hidden = false;
}


/* ---------- 6. Download ---------- */

el.btnBaixar.addEventListener('click', async () => {
    // acha o botão de rádio que está marcado
    const escolhido = document.querySelector('input[name="formato"]:checked');
    if (!escolhido || !urlAtual) return;

    el.btnBaixar.disabled = true;
    el.btnConsultar.disabled = true;
    mostrarProgresso(0, 'Iniciando o download.');
    el.painelProgresso.hidden = false;
    mostrarMensagem('info', 'Download em andamento. Não feche esta página.');

    try {
        // O servidor responde na hora com um id e continua o download
        // em segundo plano.
        const tarefa = await api('/api/downloads', postJson({
            url: urlAtual,
            formatoId: escolhido.value,
            temAudio: escolhido.dataset.temAudio === 'true',
        }));

        acompanhar(tarefa.id);

    } catch (erro) {
        finalizarDownload();
        mostrarMensagem('erro', erro.message);
    }
});

// Atualiza o número grande e a barra
function mostrarProgresso(porcentagem, texto) {
    const valor = Math.max(0, Math.min(100, porcentagem));

    el.percentual.textContent = `${Math.round(valor)}%`;
    el.barraPreenchida.style.width = `${valor}%`;
    el.barra.setAttribute('aria-valuenow', String(Math.round(valor)));
    el.progressoTexto.textContent = texto;
}

// Pergunta o andamento ao servidor a cada 1 segundo.
// Usamos setTimeout chamando a si mesma (e não setInterval) para nunca
// ter dois pedidos ao mesmo tempo se o servidor demorar.
function acompanhar(id) {
    temporizador = setTimeout(async () => {
        try {
            const tarefa = await api(`/api/downloads/${id}`);

            if (tarefa.status === 'CONCLUIDO') {
                mostrarProgresso(100, 'Download concluído.');
                finalizarDownload();
                mostrarMensagem('sucesso', 'Download concluído. O arquivo foi enviado ao seu navegador.');

                // Navegar até o endereço do arquivo faz o navegador baixá-lo
                window.location.href = `/api/downloads/${id}/file`;

            } else if (tarefa.status === 'ERRO') {
                finalizarDownload();
                mostrarMensagem('erro', tarefa.mensagem);

            } else {
                mostrarProgresso(tarefa.progresso, 'Baixando. O vídeo e o áudio podem ser baixados em duas etapas.');
                acompanhar(id);   // ainda em andamento: pergunta de novo
            }

        } catch (erro) {
            finalizarDownload();
            mostrarMensagem('erro', erro.message);
        }
    }, 1000);
}

function finalizarDownload() {
    pararAcompanhamento();
    el.btnBaixar.disabled = false;
    el.btnConsultar.disabled = false;

    // O servidor grava o histórico logo depois de concluir.
    // Esperamos um instante para a lista já incluir este download.
    setTimeout(carregarHistorico, 1000);
}


/* ---------- 7. Histórico (vem do banco de dados) ---------- */

async function carregarHistorico() {
    try {
        const itens = await api('/api/historico');

        el.listaHistorico.replaceChildren();
        el.historicoVazio.hidden = itens.length > 0;

        itens.forEach((item) => {
            const linha = document.createElement('li');

            const titulo = document.createElement('strong');
            titulo.textContent = item.nomeArquivo || item.url;

            const detalhe = document.createElement('small');
            const data = new Date(item.iniciadoEm).toLocaleString('pt-BR');

            if (item.status === 'CONCLUIDO') {
                detalhe.textContent = `Concluído em ${data}`;
            } else {
                linha.className = 'com-erro';
                detalhe.textContent = `Falhou em ${data}`;
            }

            linha.append(titulo, detalhe);
            el.listaHistorico.append(linha);
        });

        el.painelHistorico.hidden = false;

    } catch (e) {
        // O histórico é um recurso secundário. Se a API de histórico
        // não existir ainda (ou falhar), a seção fica escondida e o
        // resto da página funciona normalmente.
    }
}

// Ao abrir a página, já carrega o histórico
carregarHistorico();
