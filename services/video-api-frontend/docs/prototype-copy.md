# Inventário de textos do protótipo

Este documento cobre somente as seis telas do épico de linguagem: Entrar, Cadastrar, Meus vídeos, Detalhe do vídeo, Enviar vídeo e Meu perfil, além do shell compartilhado. Os textos vêm de `src/product-copy.ts`. Arquivos, e-mail e datas são dados, não textos literais.

## Variáveis de padrões dinâmicos

| Variável | Origem | Formatação |
| --- | --- | --- |
| `{data}` | `uploadedAt` ISO UTC | `Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })` no fuso do navegador |
| `{nome}` | `File.name` selecionado | texto simples |
| `{tamanho}` | `File.size` | KB/MB em `pt-BR` |
| `{percent}` | progresso do mock, 0 a 100 | número inteiro |

## Correspondência de estados técnicos → produto

Duas categorias aparecem juntas na lista e no detalhe.

| Origem interna (não visível) | Texto visível | Categoria |
| --- | --- | --- |
| `Video.PENDING` | pendente | Vídeo (lista) |
| `Video.UPLOADED` | enviado | Vídeo (lista) |
| `Video.REJECTED` | rejeitado | Vídeo (lista) |
| `Video.EXPIRED` | expirado | Vídeo (lista) |
| tentativa `PENDING` ou ausente | pendente | Processamento (detalhe) |
| tentativa `PROCESSING` | processando | Processamento (detalhe) |
| tentativa `COMPLETED` | completado | Processamento (detalhe) |
| tentativa `FAILED` | error | Processamento (detalhe) |

Marcos futuros mostram `Aguardando`. Dado histórico ausente mostra `Data indisponível`.

## Inventário

| Chave | Tela | Texto ou padrão | Condição | Tipo |
| --- | --- | --- | --- | --- |
| access.kicker | Entrar | Acesso | painel de acesso | fixo |
| access.heroTitle | Entrar | Extraia imagens dos seus vídeos. | painel ilustrativo | fixo |
| access.heroLead | Entrar | Envie um vídeo, acompanhe o processamento e baixe as imagens quando estiverem prontas. | painel ilustrativo | fixo |
| access.stepSend | Entrar | Envie o vídeo | etapas ilustrativas | fixo |
| access.stepProcess | Entrar | Extraia as imagens | etapas ilustrativas | fixo |
| access.stepDownload | Entrar | Baixe o pacote | etapas ilustrativas | fixo |
| access.enterTab | Entrar | Entrar | aba | fixo |
| access.createTab | Cadastrar | Criar conta | aba | fixo |
| access.loginTitle | Entrar | Entre para acompanhar seus vídeos. | formulário de entrada | fixo |
| access.loginLead | Entrar | Use seu e-mail e senha para acessar a FIAP X. | formulário de entrada | fixo |
| access.registerTitle | Cadastrar | Crie sua conta. | formulário de cadastro | fixo |

| access.emailLabel | Entrar | E-mail | campo | fixo |
| access.passwordLabel | Entrar | Senha | campo | fixo |
| access.confirmPasswordLabel | Cadastrar | Confirmar senha | campo | fixo |
| access.loginSubmit | Entrar | Entrar | ação primária | fixo |
| access.loginPending | Entrar | Entrando… | envio em andamento | fixo |
| access.registerSubmit | Cadastrar | Criar conta | ação primária | fixo |
| access.registerPending | Cadastrar | Criando conta… | envio em andamento | fixo |
| access.emailRequired | Entrar | Informe seu e-mail. | e-mail vazio | fixo |
| access.emailInvalid | Entrar | Informe um e-mail válido. | e-mail malformado | fixo |
| access.passwordRequired | Entrar | Informe sua senha. | senha vazia | fixo |
| access.passwordLength | Entrar | A senha deve ter entre 8 e 128 caracteres. | senha fora do intervalo | fixo |
| access.confirmRequired | Cadastrar | Confirme sua senha. | confirmação vazia | fixo |
| access.passwordMismatch | Cadastrar | As senhas não coincidem. | senhas diferentes | fixo |
| access.invalidCredentials | Entrar | E-mail ou senha inválidos. | credenciais rejeitadas | fixo |
| access.loginUnavailable | Entrar | Não foi possível entrar. Tente novamente. | falha inesperada | fixo |
| access.registerDuplicate | Cadastrar | Este e-mail já está cadastrado. | e-mail duplicado | fixo |
| access.registerUnavailable | Cadastrar | Não foi possível criar a conta. Tente novamente. | falha inesperada | fixo |
| access.registerSuccess | Entrar | Conta criada. Entre com o e-mail e a senha cadastrados. | após cadastro válido | fixo |

| shell.navVideos | Shell compartilhado | Meus vídeos | menu | fixo |
| shell.navUpload | Shell compartilhado | Enviar vídeo | menu | fixo |
| shell.navProfile | Shell compartilhado | Meu perfil | menu | fixo |
| shell.logout | Shell compartilhado | Sair | encerrar sessão | fixo |
| shell.openMenu | Shell compartilhado | Abrir menu | menu móvel fechado | fixo |
| shell.closeMenu | Shell compartilhado | Fechar menu | menu móvel aberto | fixo |

| status.* | Meus vídeos / Detalhe | ver tabela de estados | estado visível do vídeo | fixo |
| videos.title | Meus vídeos | Meus vídeos | cabeçalho | fixo |
| videos.lead | Meus vídeos | Acompanhe o envio e o processamento dos seus arquivos. | cabeçalho | fixo |
| videos.emptyTitle | Meus vídeos | Nenhum vídeo ainda | lista vazia | fixo |
| videos.emptyBody | Meus vídeos | Você ainda não enviou vídeos. | lista vazia | fixo |
| videos.emptyAction | Meus vídeos | Enviar o primeiro vídeo | lista vazia | fixo |
| videos.loading | Meus vídeos | Carregando seus vídeos. | carregamento | fixo |
| videos.error | Meus vídeos | Não foi possível carregar seus vídeos. | falha recuperável | fixo |
| videos.retry | Meus vídeos | Tentar de novo | falha recuperável | fixo |
| videos.openDetail | Meus vídeos | Ver detalhes | item da lista | fixo |
| videos.sentAt | Meus vídeos | Enviado em {data} | item com horário de envio | dinâmico |
| videos.loadMore | Meus vídeos | Carregar mais | próxima página | fixo |
| detail.back | Detalhe do vídeo | Voltar para meus vídeos | navegação interna | fixo |
| detail.timeline | Detalhe do vídeo | Andamento | linha do tempo | fixo |
| detail.sent | Detalhe do vídeo | Enviado em | marco | fixo |
| detail.processed | Detalhe do vídeo | Processado em | marco | fixo |
| detail.available | Detalhe do vídeo | Disponível em | marco | fixo |
| detail.awaiting | Detalhe do vídeo | Aguardando | marco futuro | fixo |
| detail.unavailable | Detalhe do vídeo | Data indisponível | dado histórico ausente | fixo |
| detail.history | Detalhe do vídeo | Processamentos anteriores | histórico | fixo |
| detail.historyEmpty | Detalhe do vídeo | Este vídeo ainda não teve outros processamentos. | sem tentativas anteriores | fixo |
| detail.download | Detalhe do vídeo | Baixar imagens | vídeo disponível | fixo |
| detail.downloadSimulated | Detalhe do vídeo | O download é simulado neste protótipo. Nenhum arquivo é gerado. | após o CTA | fixo |
| detail.notFound | Detalhe do vídeo | Não foi possível encontrar este vídeo. | vídeo inexistente para o dono | fixo |
| upload.title | Enviar vídeo | Enviar vídeo | cabeçalho | fixo |
| upload.lead | Enviar vídeo | Selecione um arquivo para simular o envio. Formatos aceitos: MP4, MOV, WebM e MKV. Limite de 500 MB (500.000.000 bytes). | instruções | fixo |
| upload.selected | Enviar vídeo | {nome} · {tamanho} | arquivo válido | dinâmico |
| upload.progress | Enviar vídeo | Enviando o vídeo… {percent}% | progresso | dinâmico |
| upload.success | Enviar vídeo | Vídeo enviado. Você já pode acompanhar o andamento em Meus vídeos. | sucesso simulado | fixo |
| upload.error | Enviar vídeo | Não foi possível enviar o vídeo. Tente novamente. | falha simulada | fixo |
| profile.title | Meu perfil | Meu perfil | cabeçalho | fixo |
| profile.email | Meu perfil | E-mail | dado da sessão | fixo |
| profile.note | Meu perfil | A edição de perfil não está disponível nesta versão. | somente leitura | fixo |
| prototype.* | Shell compartilhado | Cenários de demonstração e opções | revisão de estados | fixo |

A lista completa e canônica das chaves está em `COPY_INVENTORY` no código.
