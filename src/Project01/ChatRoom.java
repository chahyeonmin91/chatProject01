package Project01;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ChatRoom {
    private int roomNumber; // 방 번호
    private Map<String, PrintWriter> clients; // 클라이언트 목록

    // 생성자
    public ChatRoom(int roomNumber) {
        this.roomNumber = roomNumber;
        this.clients = new HashMap<>();
    }

    // 클라이언트 추가
    public void addClient(String clientName, PrintWriter writer) {
        clients.put(clientName, writer);
    }

    // 클라이언트 제거
    public boolean removeClient(String clientName) {
        return clients.remove(clientName) != null;
    }
    // 방이 비어 있는지 확인
    public boolean isEmpty() {
        return clients.isEmpty();
    }

    // 방 번호 반환
    public int getRoomNumber() {
        return roomNumber;
    }
}
