using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;

namespace GeoQuiz_backend.Application.DTOs.PvP
{
    public class MatchFoundWithDraftData
    {
        public Guid MatchId { get; set; }

        public Guid OpponentId { get; set; }
        public string OpponentName { get; set; }
        public int OpponentLevel { get; set; }
        public int OpponentScore { get; set; }
        public bool OpponentIsPremium { get; set; }


        public List<GameMode> AvailableModes { get; set; }
        public List<GameMode> BannedModes { get; set; } 
        public Guid CurrentTurnUserId { get; set; } 
        public long TimerEndAt { get; set; }
        public long ServerTime {  get; set; }
        public DateTime FirstTurnStartTime { get; set; }

        public Guid YourId { get; set; }
        public bool IsYourTurn => CurrentTurnUserId == YourId;
    }



    public class DraftUpdateData
    {
        public Guid MatchId { get; set; }
        public GameMode BannedMode { get; set; }
        public Guid BannedByUserId { get; set; } 
        public List<GameMode> RemainingModes { get; set; } 
        public Guid NextTurnUserId { get; set; } 
        public int Step { get; set; }
        public bool IsDraftCompleted { get; set; }
    }


    public class GameResumeData
    {
        public GameReadyData GameData { get; set; } = new GameReadyData();
        public string OpponentName { get; set; } = null!;
        public int OpponentTotalScore { get; set; }
        public int YourTotalScore { get; set;  }
        public int OpponentCurrentScore { get; set; }
        public int YourCurrentScore { get; set; }
        public int CurrentQuestion {  get; set; }
        public long TimerEndAt { get; set; }
        public long ServerTime { get; set; }
    }
    public class GameReadyData
    {
        public Guid MatchId { get; set; }
        public GameMode SelectedMode { get; set; }
        public int TotalQuestions { get; set; } = 10;

        public int TotalGameTimeSeconds { get; set; } = 60;
        public DateTime GameStartTime { get; set; }

        public List<QuestionData> Questions { get; set; } = new();
    }
    public class QuestionData
    {
        public string QuestionId { get; set; } = null!;
        public LocalizedText QuestionText { get; set; } = null!;
        public List<OptionData> Options { get; set; } = new();
        public int QuestionNumber { get; set; }

        public string? ImageUrl { get; set; }
        public string? AudioUrl { get; set; }
    }
    public class OptionData
    {
        public int Index { get; set; }
        public LocalizedText Text { get; set; } = null!;
    }

    public class TimerUpdateData
    {
        //public Guid MatchId { get; set; }
        //public int RemainingTimeSeconds { get; set; }
        //public DateTime ServerTime { get; set; }
        //public DateTime TimerEndsAt { get; set; }
        public long ServerTime { get; set; }
        public long TimerEndsAt { get; set; }
    }



    public class DisconnectData
    {
        public Guid MatchId { get; set; }
        public DisconnectReason Reason { get; set; } 
        public bool YouWin { get; set; } 
        public Guid DisconnectedUserId { get; set; }
        public int DisconnectedAtQuestion { get; set; }
        public int YourCurrentScore { get; set; }
        public int OpponentCurrentScore { get; set; }

        public GameFinishedData? EarlyFinishData { get; set; }

    }
    public enum DisconnectReason
    {
        Timeout = 1,           
        Manual = 2,            
        ConnectionLost = 3,    
        GameError = 4          
    }



    public class GameFinishedData
    {
        public Guid MatchId { get; set; }
        public Guid WinnerId { get; set; }

        public GameFinishReason FinishReason { get; set; }
        public PlayerFinalStats YourStats { get; set; } = null!;
        public PlayerFinalStats OpponentStats { get; set; } = null!;

        public int ExperienceGained { get; set; }
        public List<UnlockedAchievement> UnlockedAchievements { get; set; } = new();
    }

    public class PlayerFinalStats
    {
        public Guid UserId { get; set; }
        public int FinalScore { get; set; }
        public int CorrectAnswers { get; set; }
        public int TotalQuestionsAnswered { get; set; } 
        public double AverageAnswerTimeMs { get; set; }
    }

    public enum GameFinishReason
    {
        AllQuestionsAnswered = 1,  
        TimeOut = 2,                
        OpponentDisconnected = 3,
        PlayerDisconnected = 4
    }
    public class UnlockedAchievement
    {
        public string Code { get; set; } = null!;
        public string Title { get; set; } = null!;
        public string Description { get; set; } = null!;
        public string Icon { get; set; } = null!;
        public AchievementRarity Rarity { get; set; }
    }

}
