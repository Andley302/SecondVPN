# Second VPN

## Sobre
Realize conexões VPN sob o protocolo ssh2 usando https, proxy http ou proxy https.

## Descrição
Funcionamento básico do app:

1. O usuário irá inserir uma payload (que nada mais é que a estrutura de uma requisição qualquer) e o método de conexão (que pode ser https ou http) e juntamente com o servidor que irá realizar a conexão ssh.
2. O app irá formatar a requisição e tentar realizar a conexão sob o proxy inserido.
3. Se bem sucedido, a autenticação do ssh será iniciada.
4. Se autenticado, iniciamos um servidor socks local, e iniciamos o VPNService do android para usar o mesmo.
5. Todo tráfego do dispositivo será tunelado para o socks local, que por sua vez estará ligado ao servidor ssh remoto.

## Instalação
Para compilar o projeto, siga estas etapas:

1. Clone o repositório.
2. Importe-o no Android Studio.

