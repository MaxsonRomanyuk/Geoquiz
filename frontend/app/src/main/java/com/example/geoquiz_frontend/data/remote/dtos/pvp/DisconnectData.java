package com.example.geoquiz_frontend.data.remote.dtos.pvp;

public class DisconnectData {
    private String matchId;
    private String reason;
    private boolean youWin;
    private String disconnectedUserId;
    private int disconnectedAtQuestion;
    private int yourCurrentScore;
    private int opponentCurrentScore;
    private GameFinishedData earlyFinishData;

    public String getMatchId() { return matchId; }
    public String getReason() { return reason; }
    public boolean isYouWin() { return youWin; }
    public String getDisconnectedUserId() { return disconnectedUserId; }
    public int getDisconnectedAtQuestion() { return disconnectedAtQuestion; }
    public int getYourCurrentScore() { return yourCurrentScore; }
    public int getOpponentCurrentScore() { return opponentCurrentScore; }
    public GameFinishedData getEarlyFinishData() { return earlyFinishData; }

    public boolean isTimeout() { return "Timeout".equals(reason); }
    public boolean isManual() { return "Manual".equals(reason); }
    public boolean isConnectionLost() { return "ConnectionLost".equals(reason); }
    public boolean isGameError() { return "GameError".equals(reason); }
}
