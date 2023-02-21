package com.dsware.om.client.common;

import com.dsware.om.client.exception.DSwareErrorCode;
import com.dsware.om.client.exception.DSwareException;
import com.dsware.om.client.util.CommonUtils;

public class MessageDispatcher {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageDispatcher.class);

  private static MessageDispatcher instance = null;
  private int port = Constants.API_PORT;
  private int timeout = Constants.CHANNEL_TIME_OUT;

  private int readtimeout = Constants.CHANNEL_READ_TIME_OUT;

  private int allRetryTime = Constants.SEND_MESSAGE_RETRY_TIME;

  private int retrySleepTime = Constants.SEND_MESSAGE_SLEEP_TIME;

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public static synchronized MessageDispatcher getInstance() {
    if (null == instance) {
      instance = new MessageDispatcher();
    }
    return instance;
  }

  private ResponseMessage sendMessageToOneAgent(String agentIp, RequestMessage request) {
    ResponseMessage response = null;
    Socket socket = null;
    DataOutputStream dos = null;
    DataInputStream dis = null;

    try {
      if (port == DSwareSSLContext.getInstance().getAgentSSLPort()) {
        SSLContext sslContext = DSwareSSLContext.getInstance().getSslContext();
        socket = sslContext.getSocketFactory().createSocket();
      } else {
        socket = new Socket();
      }
      InetSocketAddress addr = new InetSocketAddress(agentIp, port);
      // 设置该socket的连接超时时间 3000ms
      socket.connect(addr, timeout);
      // 设置该socket的读数据超时时间 35s
      socket.setSoTimeout(readtimeout);

      // 获取输出流，用于客户端向服务器端发送数据
      dos = new DataOutputStream(socket.getOutputStream());
      dis = new DataInputStream(socket.getInputStream());

      ByteBuffer buffer = CommonUtils.messageToBuffer(request, ByteOrder.BIG_ENDIAN);
      dos.write(buffer.array());
      dos.flush();

      ResponseMessage result;
      LOGGER.debug("readMsgFromChannel begin");

      if (null == socket) {
        throw new DSwareException(DSwareErrorCode.INTERNAL_ERROR, "[ResponseMessage] parameter 'socket' is null");
      }
      if (null == dis) {
        throw new DSwareException(DSwareErrorCode.INTERNAL_ERROR, "[ResponseMessage] parameter 'din' is null");
      }
      if (null == ByteOrder.LITTLE_ENDIAN) {
        throw new DSwareException(
            DSwareErrorCode.INTERNAL_ERROR, "[ResponseMessage] parameter 'peerOrder' is null");
      }

      ResponseMessage msg = new ResponseMessage();
      LOGGER.debug("readRawByteFromChannel begin");

      final int bufSize1 = (CommonUtils.NET_MSG_HEADER_FIELD_LEN
          + CommonUtils.NET_MSG_LENGTH_FIELD_LEN >= CommonUtils.NET_BUFFER_SIZE) ? CommonUtils.NET_BUFFER_SIZE
              : CommonUtils.NET_MSG_HEADER_FIELD_LEN + CommonUtils.NET_MSG_LENGTH_FIELD_LEN;

      ByteBuffer buf1 = ByteBuffer
          .allocate(CommonUtils.NET_MSG_HEADER_FIELD_LEN + CommonUtils.NET_MSG_LENGTH_FIELD_LEN);
      ByteBuffer recvBuf1 = ByteBuffer.allocate(bufSize1);

      int recvLen1 = 0;
      int total1 = 0;

      while (total1 < CommonUtils.NET_MSG_HEADER_FIELD_LEN + CommonUtils.NET_MSG_LENGTH_FIELD_LEN) {
        try {
          // 阻塞式读取
          recvLen1 = dis.read(recvBuf1.array());
        } catch (SocketTimeoutException e1) {
          LOGGER.error(
              "[readRawByteFromChannel] failed to read from socket caused by socket_read_timeout "
                  + e1.getMessage());
          throw new DSwareException(
              DSwareErrorCode.SOCKET_READ_TIMEOUT,
              e1,
              "failed to read from socket caused by socket_read_timeout" + socket.getRemoteSocketAddress());
        } catch (Exception e1) {
          LOGGER.error("[readRawByteFromChannel] failed to read from socket caused by Exception");
          throw new DSwareException(
              DSwareErrorCode.INTERNAL_ERROR,
              e1,
              "failed to read from socket" + socket.getRemoteSocketAddress());
        }

        // 读出错则直接报错
        if (0 >= recvLen1) {
          LOGGER.error(
              "[readRawByteFromChannel] Failed to read from Agent "
                  + socket.getRemoteSocketAddress()
                  + " current recv len="
                  + total1);
          throw new DSwareException(DSwareErrorCode.COMMUNICATE_AGENT_ERROR, "read from agent error");
        } else {
          System.arraycopy(recvBuf1.array(), 0, buf1.array(), total1, recvLen1);
          total1 += recvLen1;
          recvBuf1.clear();
        }
      }
      buf1.position(CommonUtils.NET_BUFFER_START_POS);
      LOGGER.debug("readRawByteFromChannel end");
      ByteBuffer header = buf1;
      // header 不会为空
      header.position(CommonUtils.NET_BUFFER_START_POS);
      header.order(ByteOrder.LITTLE_ENDIAN);

      msg.setHeader(header.getInt());
      long length = CommonUtils.unsignedIntToLong(header.getInt());

      if (length <= 0) {
        LOGGER.error("Message length is 0 , no message body");
        result = msg;
      } else if (length > msg.getMaxMsgSize()) {
        LOGGER.error("Message length more than 32M , no message body");
        result = msg;
      } else {
        msg.setLength((int) length);
        LOGGER.debug("readRawByteFromChannel begin");

        final int bufSize = ((int) length >= CommonUtils.NET_BUFFER_SIZE) ? CommonUtils.NET_BUFFER_SIZE : (int) length;

        ByteBuffer buf = ByteBuffer.allocate((int) length);
        ByteBuffer recvBuf = ByteBuffer.allocate(bufSize);

        int recvLen = 0;
        int total = 0;

        while (total < (int) length) {
          try {
            // 阻塞式读取
            recvLen = dis.read(recvBuf.array());
          } catch (SocketTimeoutException e) {
            LOGGER.error(
                "[readRawByteFromChannel] failed to read from socket caused by socket_read_timeout "
                    + e.getMessage());
            throw new DSwareException(
                DSwareErrorCode.SOCKET_READ_TIMEOUT,
                e,
                "failed to read from socket caused by socket_read_timeout" + socket.getRemoteSocketAddress());
          } catch (Exception e) {
            LOGGER.error("[readRawByteFromChannel] failed to read from socket caused by Exception");
            throw new DSwareException(
                DSwareErrorCode.INTERNAL_ERROR,
                e,
                "failed to read from socket" + socket.getRemoteSocketAddress());
          }

          // 读出错则直接报错
          if (0 >= recvLen) {
            LOGGER.error(
                "[readRawByteFromChannel] Failed to read from Agent "
                    + socket.getRemoteSocketAddress()
                    + " current recv len="
                    + total);
            throw new DSwareException(DSwareErrorCode.COMMUNICATE_AGENT_ERROR, "read from agent error");
          } else {
            System.arraycopy(recvBuf.array(), 0, buf.array(), total, recvLen);
            total += recvLen;
            recvBuf.clear();
          }
        }
        buf.position(CommonUtils.NET_BUFFER_START_POS);
        LOGGER.debug("readRawByteFromChannel end");
        ByteBuffer body = buf;// body 不会为空
        msg.setMsgBody(body.array());
        msg.resetPos();
        LOGGER.debug("readMsgFromChannel end");
        result = msg;
      }

      response = result;

      // LOGGER.debug("[sendMessageToAgent] ResponseMsg:" + response);
      int ret = response.getHeader();
      if (Constants.RET_SUCCESS == ret) {
        return response;
      } else {
        LOGGER.error("Failed to send message to vbs, agent = {}, errorCode = {}", agentIp, ret);
        return response;
      }

    } catch (SocketTimeoutException e) {
      LOGGER.error("socket connect timeout, ip=" + agentIp, e);
    } catch (IOException e) {
      LOGGER.error("Send message to agent io error, ip=" + agentIp + ", msg=" + request);
      LOGGER.error("socket IOException:" + e);
    } catch (Exception e) {
      LOGGER.error("Failed to send message to agent=" + agentIp + ", msg=" + request);
      LOGGER.error("Original exception:" + e);
    } finally {
      closeSocket(socket);
      closeDos(dos);
      closeDis(dis);
    }
    LOGGER.error("[sendMessageToOneAgent] no response messsage from one agent!");
    return null;
  }

  private boolean isNeedRetryCode(DSwareErrorCode errorCodeRet) {
    boolean ret = false;
    if (DSwareErrorCode.NO_RESPONSE_ERROR == errorCodeRet
        || DSwareErrorCode.AGENT_ERR_NO_AVALIABLE_VBS == errorCodeRet
        || DSwareErrorCode.AGENT_CONNECT_ERROR == errorCodeRet) {
      ret = true;
      return ret;
    }

    if (DSwareErrorCode.SOCKET_CONNECT_TIMEOUT == errorCodeRet
        || DSwareErrorCode.SOCKET_READ_TIMEOUT == errorCodeRet
        || DSwareErrorCode.COMMUNICATE_AGENT_ERROR == errorCodeRet
        || DSwareErrorCode.DSW_VBS_UNKNOW_MAJOR_VBS == errorCodeRet) {
      ret = true;
      return ret;
    }

    return ret;
  }

  public ResponseMessage sendMessageToAgent(String[] agentIp, RequestMessage request) {
    // 入参校验
    if (null == agentIp || null == request) {
      LOGGER.error("[sendMessageToAgent] input agent ip or request is null!");
      throw new DSwareException(DSwareErrorCode.INVALID_PARAMETER);
    }
    ResponseMessage response = null;
    // LOGGER.debug("[sendMessageToAgent] RequestMsg:" + request);
    int retryTime = 0;
    while (retryTime < allRetryTime) {
      for (int i = 0; i < agentIp.length; i++) {
        response = sendMessageToOneAgent(agentIp[i], request);
        if (null != response) {
          int ret = response.getHeader();
          if (Constants.RET_SUCCESS == ret) {
            return response;
          }
          DSwareErrorCode errorCodeRet = DSwareErrorCode.toEnum(ret);
          if (!isNeedRetryCode(errorCodeRet)) {
            return response;
          }
        }
      }

      LOGGER.info("vbs is starting,retry time: {}", retryTime);

      try {
        TimeUnit.SECONDS.sleep(retrySleepTime);
      } catch (InterruptedException e) {
        LOGGER.error("Sleep exception : " + e.getMessage());
      }

      retryTime = retryTime + 1;
    }

    // 如果获取多次结果为null，则定义为内部错误
    LOGGER.error("[sendMessageToAgent] no response messsage from agent!");
    throw new DSwareException(DSwareErrorCode.NO_RESPONSE_ERROR);
  }
}