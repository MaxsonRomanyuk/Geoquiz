using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Application.DTOs.PvP
{
    public class MatchFoundWithDraftData
    {
        public Guid MatchId { get; set; }

        public Guid OpponentId { get; set; }
        public string OpponentName { get; set; }
        public int OpponentLevel { get; set; }
        public bool OpponentIsPremium { get; set; }


        public List<GameMode> AvailableModes { get; set; }
        public List<GameMode> BannedModes { get; set; } 
        public Guid CurrentTurnUserId { get; set; } 
        public int TimePerTurnSeconds { get; set; } = 10;

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



    public class GameReadyData
    {
        public Guid MatchId { get; set; }
        public GameMode SelectedMode { get; set; }
        public AppLanguage Language { get; set; }
        public int TotalQuestions { get; set; } = 10;

        public int TotalGameTimeSeconds { get; set; } = 60;
        public DateTime GameStartTime { get; set; }

        public List<QuestionData> Questions { get; set; } = new();
        public int QuestionSeed { get; set; }
        public int DifficultyLevel { get; set; }
    }
    public class QuestionData
    {
        public string QuestionId { get; set; } = null!;
        public int QuestionNumber { get; set; }
        public string QuestionText { get; set; } = null!;
        public List<OptionData> Options { get; set; } = new();

        public string? ImageUrl { get; set; }
        public string? AudioUrl { get; set; }
    }
    public class OptionData
    {
        public int Index { get; set; }
        public string Text { get; set; }
    }



    public class QuestionResultData
    {
        public Guid MatchId { get; set; }
        public int QuestionNumber { get; set; }
        public int CorrectOptionIndex { get; set; }

        public PlayerRoundResult YourResult { get; set; }
        public PlayerRoundResult OpponentResult { get; set; }

        public int YourTotalScore { get; set; }
        public int OpponentTotalScore { get; set; }
        public int YourCorrectCount { get; set; }
        public int OpponentCorrectCount { get; set; }

        public int RemainingTimeSeconds { get; set; }
        public bool IsLastQuestion { get; set; }
    }
    public class PlayerRoundResult
    {
        public bool HasAnswered { get; set; } 
        public bool IsCorrect { get; set; } 
        public int TimeSpentMs { get; set; } 
        public int ScoreGained { get; set; }
    }



    public class TimerUpdateData
    {
        public Guid MatchId { get; set; }
        public int RemainingTimeSeconds { get; set; }
        public DateTime ServerTime { get; set; } 
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
