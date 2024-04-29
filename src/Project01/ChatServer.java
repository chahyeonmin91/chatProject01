package Project01;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ChatServer {
    private static final String COMMAND_LIST = "방 목록 보기 : /list\n방 생성 : /create\n방 입장 : /join [방번호]\n방 나가기 : /exit\n접속종료 : /bye";
    private static final String CHAT_HISTORY_DIR = "chat_history/";

    static ArrayList<ChatRoom> chatRooms = new ArrayList<>();

    public static void main(String[] args) {
        //1. 서버소켓을 생성!!
        try (ServerSocket serverSocket = new ServerSocket(12345);) {
            System.out.println("서버가 준비되었습니다.");
            //여러명의 클라이언트의 정보를 기억할 공간
            Map<String, PrintWriter> chatClients = new HashMap<>();

            while (true) {
                //2. accept() 를 통해서 소켓을 얻어옴.   (여러명의 클라이언트와 접속할 수 있도록 구현)
                Socket socket = serverSocket.accept();
                //Thread 이용!!
                //여러명의 클라이언트의 정보를 기억할 공간
                new ChatThread(socket, chatClients).start();

            }
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
        chatRooms.add(new ChatRoom(chatRooms.size() + 1));
    }
}

class ChatThread extends Thread {
    //생성자를 통해서 클라이언트 소켓을 얻어옴.
    private Socket socket;
    private String id;
    private Map<String, PrintWriter> chatClients;
    private int userRoomIndex;

    private BufferedReader in;
    PrintWriter out;

    public ChatThread(Socket socket, Map<String, PrintWriter> chatClients) {
        this.socket = socket;
        this.chatClients = chatClients;

        //클라이언트가 생성될 때 클라이언트로 부터 아이디를 얻어오게 하고 싶어요.
        //각각 클라이언트와 통신 할 수 있는 통로얻어옴.
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //Client가 접속하자마 id를 보낸다는 약속!!
            id = in.readLine();

            while (true) {
                synchronized (chatClients) {
                    if (chatClients.containsKey(id)) {
                        // 이미 사용 중인 닉네임이라면
                        out.println("중복된 닉네임입니다. 다른 닉네임을 입력하세요.");
                        // 사용자에게 다른 닉네임을 요구합니다.
                        id = in.readLine(); // 새로운 닉네임을 받음
                    } else {
                        // 사용자를 채팅에 추가합니다.
                        chatClients.put(this.id, out);
                        break; // 중복된 닉네임이 없으면 반복문 종료
                    }
                }
            }

            //이때..  모든 사용자에게 id님이 입장했다라는 정보를 알려줌.
            broadcast(id + " 닉네임의 사용자가 연결했습니다.");
            System.out.println("새로운 사용자의 아이디는 " + id + "입니다.");
            // 클라이언트의 IP 주소 출력
            InetAddress clientAddress = socket.getInetAddress();
            broadcast("새로운 연결: " + clientAddress.getHostAddress());
            System.out.println("새로운 연결: " + clientAddress.getHostAddress());

            //동시에 일어날 수도..
            synchronized (chatClients) {
                chatClients.put(this.id, out);
            }
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println(id + "사용자 채팅시작!!");
        String msg = null;
        try {
            while ((msg = in.readLine()) != null) {
                if ("/bye".equalsIgnoreCase(msg))
                    break;

                if (msg.indexOf("/whisper") == 0)
                {sendMsg(msg);
                    continue;
                }


                if (msg.startsWith("/create")) {
                    createRoom();
                } else if (msg.startsWith("/list")) {
                    listRooms();
                } else if (msg.startsWith("/join")) {
                    joinRoom(msg);
                } else if (msg.startsWith("/exit")) {
                    exitRoom();
                } else
                    broadcast(id + " : " + msg);
            }
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        } finally {
            synchronized (chatClients) {
                chatClients.remove(id);
            }
            broadcast(id + " 닉네임의 사용자가 연결을 끊었습니다.");

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    //메시지를 특정 사용자에게만 보내는 메서드
    public void sendMsg(String msg) {
        int firstSpaceIndex = msg.indexOf(" ");
        if (firstSpaceIndex == -1) return; //공백이 없다면....

        int secondSpaceIndex = msg.indexOf(" ", firstSpaceIndex + 1);
        if (secondSpaceIndex == -1) return; //두번재 공백이 없다는 것도 메시지가 잘못된거니까..

        String whisper = msg.substring(firstSpaceIndex + 1, secondSpaceIndex);
        String message = msg.substring(secondSpaceIndex + 1);

        //to(수신자)에게 메시지 전송.
        PrintWriter pw = chatClients.get(whisper);
        if (pw != null) {
            pw.println(id + "님으로부터 온 비밀 메시지 : " + message);
        } else {
            System.out.println("오류 : 수신자 " + whisper + " 님을 찾을 수 없습니다.");
        }
    }

    //메지시를 전체 사용자에게 보내는 메서드
    public void broadcast(String msg) {
//        for (PrintWriter out : chatClients.values()) {
//            out.println(msg);
////            out.flush();
//        }
        synchronized (chatClients) {
            Iterator<PrintWriter> it = chatClients.values().iterator();
            while (it.hasNext()) {
                PrintWriter out = it.next();
                try {
                    out.println(msg);
                } catch (Exception e) {
                    it.remove();  //브로드케스트 할 수 없는 사용자를 제거한다.
                    e.printStackTrace();
                }
            }
        }
    }

    // 새로운 방을 생성하는 메서드
    private void createRoom() {
        // 새로운 방을 생성하고 방 목록에 추가합니다.
        ChatRoom newRoom = new ChatRoom(ChatServer.chatRooms.size() + 1);
        ChatServer.chatRooms.add(newRoom);
        // 생성된 방 정보를 현재 클라이언트에게 전송합니다.
        out.println("방 번호 " + newRoom.getRoomNumber() + "가 생성되었습니다.");
    }

    // 모든 방의 목록을 출력하는 메서드
    private void listRooms() {
        StringBuilder roomList = new StringBuilder("방 목록:\n");
        for (ChatRoom room : ChatServer.chatRooms) {
            roomList.append("방 번호 ").append(room.getRoomNumber()).append("\n");
        }
        out.println(roomList.toString());
    }

    // 특정 방에 입장하는 메서드
    private void joinRoom(String msg) {
        // msg에서 방 번호를 파싱합니다.
        String[] parts = msg.split(" ");
        if (parts.length == 2) {
            try {
                int roomNumber = Integer.parseInt(parts[1]);
                // 해당 번호의 방이 존재하면 클라이언트를 해당 방으로 이동시킵니다.
                if (roomNumber >= 1 && roomNumber <= ChatServer.chatRooms.size()) {
                    // 이동할 방의 인덱스는 -1 처리합니다.
                    int roomIndex = roomNumber - 1;
                    // 이전 방에서 현재 클라이언트를 제거합니다.
                    // 이전 방이 더 이상 사용자를 포함하지 않으면 방을 삭제합니다.
                    ChatRoom previousRoom = ChatServer.chatRooms.get(userRoomIndex);
                    if (previousRoom.removeClient(id) && previousRoom.isEmpty()) {
                        ChatServer.chatRooms.remove(previousRoom);
                        out.println("방 번호 " + previousRoom.getRoomNumber() + "가 삭제되었습니다.");
                    }
                    // 새로운 방에 클라이언트를 추가하고 클라이언트에게 입장을 알립니다.
                    ChatRoom newRoom = ChatServer.chatRooms.get(roomIndex);
                    newRoom.addClient(id, out);
                    out.println(id + "님이 방 번호 " + newRoom.getRoomNumber() + "에 입장했습니다.");
                    // 클라이언트의 현재 방 인덱스를 업데이트합니다.
                    userRoomIndex = roomIndex;
                } else {
                    out.println("오류: 존재하지 않는 방 번호입니다.");
                }
            } catch (NumberFormatException e) {
                out.println("오류: 잘못된 방 번호 형식입니다.");
            }
        } else {
            out.println("오류: 올바른 방 번호를 입력하세요.");
        }
    }

    // 현재 방에서 나가는 메서드
    private void exitRoom() {
        // 현재 방에서 클라이언트를 제거하고, 방이 더 이상 사용자를 포함하지 않으면 방을 삭제합니다.
        ChatRoom currentRoom = ChatServer.chatRooms.get(userRoomIndex);
        if (currentRoom.removeClient(id) && currentRoom.isEmpty()) {
            ChatServer.chatRooms.remove(currentRoom);
            out.println("방 번호 " + currentRoom.getRoomNumber() + "가 삭제되었습니다.");
        }
        // 로비로 이동합니다.
        // 클라이언트의 현재 방 인덱스를 초기화합니다.
        userRoomIndex = -1;
    }

}
