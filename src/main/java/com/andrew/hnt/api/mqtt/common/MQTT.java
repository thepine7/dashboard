package com.andrew.hnt.api.mqtt.common;

import lombok.SneakyThrows;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQTT implements MqttCallback {

    private static final Logger logger = LoggerFactory.getLogger(MQTT.class);

    private String Broker;
    private String Client_ID;
    private String UserName;
    private String Password;
    private MqttAsyncClient Client;
    private MqttMessage message;
    private MemoryPersistence persistence;
    private MqttConnectOptions connOpts;
    private String topic;

    // 하드코딩된 설정 제거 - MqttConfig에서 주입받도록 변경

	
    	private String receiveMsg;
	private String receiveTopic;
	private volatile boolean hasNewMessage = false;  // 새 메시지 플래그 추가

	public MQTT(String broker, String client_id, String userName, String password) {
		this.Broker = broker;
		this.Client_ID = client_id;
		this.UserName = userName;
		this.Password = password;
	}
	
	public void init(String topic, String gu) {
		this.topic = topic;
		this.persistence = new MemoryPersistence();
		
		try {
			Client = new MqttAsyncClient(this.Broker, this.Client_ID, this.persistence);

			if(null != Client) {
				if(!Client.isConnected()) {
					Client.setCallback(this);
					connOpts = new MqttConnectOptions();
					connOpts.setUserName(this.UserName);
					connOpts.setPassword(this.Password.toCharArray());
					connOpts.setCleanSession(true);
                    // 서버와의 연결 유지 (초)
                    connOpts.setKeepAliveInterval(60);
					connOpts.setMqttVersion(4);
				}
			}
        // 연결 시도 및 완료 대기 (10초 타임아웃)
				Client.connect(connOpts).waitForCompletion(10000);
				logger.info("MQTT 연결 완료 대기 성공");
				
				message = new MqttMessage();
			
		} catch(MqttException mqe) {
			logger.error("MQTT 연결 실패", mqe);
		}

        // 연결 상태 확인 및 구독
        if(Client != null && Client.isConnected() && "Y".equals(gu)) {
            // 구독이 필요한 경우에만 구독 수행
            this.subscribe(0);
        }
	}
	
	public void disconnect() {
		try {
			if (Client != null) {
				logger.info("MQTT 클라이언트 연결 해제 시작");
				
				// 1. 구독 해제
				if (Client.isConnected() && topic != null && !topic.isEmpty()) {
					try {
						Client.unsubscribe(topic);
						logger.info("MQTT 토픽 구독 해제 완료: {}", topic);
					} catch (Exception e) {
						logger.warn("MQTT 토픽 구독 해제 중 오류: {}", e.getMessage());
					}
				}
				
				// 2. 연결 해제 (단계별)
				if (Client.isConnected()) {
					try {
						// 정상 연결 해제 시도 (5초 타임아웃)
						Client.disconnect(5000);
						logger.info("MQTT 정상 연결 해제 완료");
					} catch (Exception e) {
						logger.warn("MQTT 정상 연결 해제 실패: {}", e.getMessage());
					}
					
					// 강제 연결 해제 (2초 타임아웃)
					try {
						Client.disconnectForcibly(2000, 2000);
						logger.info("MQTT 강제 연결 해제 완료");
					} catch (Exception e) {
						logger.warn("MQTT 강제 연결 해제 실패: {}", e.getMessage());
					}
				}
				
				// 3. 클라이언트 완전 정리
				try {
					Client.close();
					logger.info("MQTT 클라이언트 close() 완료");
				} catch (Exception e) {
					logger.warn("MQTT 클라이언트 close() 실패: {}", e.getMessage());
				}
				
				// 4. 내부 스레드 정리를 위한 대기 (5초로 증가)
				try {
					Thread.sleep(5000); // 5초 대기
					logger.info("MQTT 내부 스레드 정리 대기 완료");
				} catch (InterruptedException e) {
					logger.warn("MQTT 스레드 정리 대기 중 인터럽트 발생");
					Thread.currentThread().interrupt();
				}
				
				// 5. 참조 해제
				Client = null;
				persistence = null;
				connOpts = null;
				message = null;
				
				logger.info("MQTT 클라이언트 완전 정리 완료");
			}
		} catch(Exception e) {
			logger.error("MQTT 연결 해제 중 오류: {}", e.getMessage(), e);
		} finally {
			// 최종 정리 보장
			Client = null;
			persistence = null;
			connOpts = null;
			message = null;
		}
	}
	
	/**
	 * MQTT 클라이언트 강제 종료 (메모리 누수 방지)
	 */
	public void forceShutdown() {
		try {
			logger.info("MQTT 클라이언트 강제 종료 시작");
			
			if (Client != null) {
				// 1. 즉시 연결 해제 (타임아웃 없음)
				try {
					if (Client.isConnected()) {
						Client.disconnectForcibly(0, 0);
						logger.info("MQTT 즉시 연결 해제 완료");
					}
				} catch (Exception e) {
					logger.warn("MQTT 즉시 연결 해제 실패: {}", e.getMessage());
				}
				
				// 2. 클라이언트 강제 종료
				try {
					Client.close();
					logger.info("MQTT 클라이언트 강제 close() 완료");
				} catch (Exception e) {
					logger.warn("MQTT 클라이언트 강제 close() 실패: {}", e.getMessage());
				}
				
				// 3. 내부 스레드 정리를 위한 대기 (10초)
				try {
					Thread.sleep(10000); // 10초 대기
					logger.info("MQTT 내부 스레드 강제 정리 대기 완료");
				} catch (InterruptedException e) {
					logger.warn("MQTT 스레드 강제 정리 대기 중 인터럽트 발생");
					Thread.currentThread().interrupt();
				}
			}
			
			// 4. 모든 참조 강제 해제
			Client = null;
			persistence = null;
			connOpts = null;
			message = null;
			topic = null;
			receiveMsg = null;
			receiveTopic = null;
			hasNewMessage = false;
			
			logger.info("MQTT 클라이언트 강제 종료 완료");
			
		} catch (Exception e) {
			logger.error("MQTT 강제 종료 중 오류: {}", e.getMessage(), e);
		} finally {
			// 최종 정리 보장
			Client = null;
			persistence = null;
			connOpts = null;
			message = null;
			topic = null;
			receiveMsg = null;
			receiveTopic = null;
			hasNewMessage = false;
		}
	}
	
	/**
	 * MQTT 연결 상태 확인
	 * @return 연결 상태
	 */
	public boolean isConnected() {
		try {
			return Client != null && Client.isConnected();
		} catch (Exception e) {
			logger.warn("MQTT 연결 상태 확인 중 오류 발생", e);
			return false;
		}
	}
	
	public void publish(String msg, int qos, String sendTopic) {
		message.setQos(qos);
		message.setPayload(msg.getBytes());

		if(Client.isConnected()) {
			try {
				if (null != sendTopic && !"".equals(sendTopic)) {
					Client.publish(sendTopic, message);
				} else {
					Client.publish(topic, message);
				}
			} catch (MqttPersistenceException mpe) {
				logger.error("MQTT 클라이언트 정리 중 오류", mpe);
			} catch (MqttException mqe) {
				logger.error("MQTT 연결 실패", mqe);
			}
		}
	}
	
	public void subscribe(int qos) {
		try {
			Client.subscribe(topic, qos);
		} catch(MqttException mqe) {
			logger.error("MQTT 구독 오류: {}", mqe.getMessage(), mqe);
		}
	}
	
	public String getTopic() {
		return topic;
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
		// MqttService는 Spring Bean으로 주입받아야 하므로 여기서는 직접 호출하지 않음
		// 대신 메시지를 저장하고 나중에 처리
		if(mqttMessage != null && mqttMessage.getPayload() != null && mqttMessage.getPayload().length > 0) {
			this.receiveMsg = new String(mqttMessage.getPayload());
			this.receiveTopic = topic;
			this.hasNewMessage = true;  // 새 메시지 플래그 설정
			
			if(this.receiveMsg.contains("setres")) {
				this.setMsg(this.receiveMsg, topic);
			}
		}
	}
	
	public void setMsg(String msg, String rcvTopic) {
		if(msg != null && !msg.trim().isEmpty()) {
			this.receiveMsg = msg;
			this.receiveTopic = rcvTopic;
			this.hasNewMessage = true;  // 플래그 설정
		}
	}
	
	public String getMsg() {
		return this.receiveMsg;
	}

	public String getRcvTopic() {
		return this.receiveTopic;
	}
	
	public boolean hasNewMessage() {
		return this.hasNewMessage;
	}
	
	public void clearNewMessageFlag() {
		this.hasNewMessage = false;
	}
	
	@SneakyThrows
	@Override
	public void connectionLost(Throwable cause) {
        logger.warn("MQTT 연결 끊김: {}", cause != null ? cause.getMessage() : "unknown");
        
        // 기존 클라이언트 완전 정리 (강화된 정리)
        try {
            if (Client != null) {
                logger.info("MQTT 연결 끊김 - 클라이언트 정리 시작");
                
                // 강제 종료로 내부 스레드 완전 정리
                try {
                    forceShutdown();
                    logger.info("MQTT 연결 끊김 - 강제 종료 완료");
                } catch (Exception e) {
                    logger.warn("MQTT 연결 끊김 - 강제 종료 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("MQTT 클라이언트 정리 중 오류 발생: {}", e.getMessage());
        }

        // 단순화된 재연결 로직 (최대 3회 시도로 제한)
        int maxAttempts = 3;
        int baseDelay = 3000; // 3초
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // 지수 백오프 지연 (3초, 6초, 12초)
                int delay = Math.min(baseDelay * attempt, 12000);
                logger.info("MQTT 재연결 시도 {}회 ({}초 후)", attempt, delay / 1000);
                Thread.sleep(delay);
                
                // 새 클라이언트 생성 (고유 ID 사용)
                String newClientId = this.Client_ID + "_" + System.currentTimeMillis() + "_" + attempt;
                Client = new MqttAsyncClient(this.Broker, newClientId, this.persistence);
                Client.setCallback(this);
                
                // 연결 옵션 설정
                connOpts = new MqttConnectOptions();
                connOpts.setUserName(this.UserName);
                connOpts.setPassword(this.Password.toCharArray());
                connOpts.setCleanSession(true);
                connOpts.setKeepAliveInterval(60);
                connOpts.setMqttVersion(4);
                connOpts.setConnectionTimeout(15); // 15초 연결 타임아웃
                connOpts.setAutomaticReconnect(false); // 자동 재연결 비활성화
                connOpts.setMaxInflight(100); // 최대 인플라이트 메시지 수 제한
                
                // 연결 시도 (15초 대기)
                Client.connect(connOpts).waitForCompletion(15000);
                
                // 구독 복원
                if (this.topic != null && !this.topic.isEmpty()) {
                    this.subscribe(0);
                }
                
                logger.info("MQTT 재연결 성공 ({}회 시도)", attempt);
                return; // 성공 시 종료
                
            } catch (Exception e) {
                logger.warn("MQTT 재연결 실패 (시도 {}회): {}", attempt, e.getMessage());
                
                // 실패 시 클라이언트 정리
                try {
                    if (Client != null) {
                        Client.close();
                        Client = null;
                    }
                } catch (Exception ex) {
                    logger.warn("MQTT 재연결 실패 후 클라이언트 정리 중 오류: {}", ex.getMessage());
                }
                
                // 마지막 시도에서도 실패하면 포기
                if (attempt == maxAttempts) {
                    logger.error("MQTT 재연결 포기 (최대 시도 횟수 초과)");
                    break;
                }
            }
        }
        
        // 메시지 객체 재생성
        if (message == null) {
            message = new MqttMessage();
        }
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
	}

}
