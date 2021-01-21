# chat

简单的聊天软件，多个客户端间通过服务器使用socket通信

## 运行方法
### 客户端
* 修改client.properties文件
* 执行命令

  ```
  java -cp chatClient.jar com.sd.client.Client
  ```
  或
  ```
  java -jar chatClient.jar
  ```
### 服务器
* 修改server.properties文件
* 执行命令

  ```
  nohup java -cp chatServer.jar com.sd.server.Server &
  ```
  或
  ```
  nohup java -jar chatServer.jar &
  ```
