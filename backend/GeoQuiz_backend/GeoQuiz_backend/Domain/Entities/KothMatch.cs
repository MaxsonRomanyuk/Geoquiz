using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;

namespace GeoQuiz_backend.Domain.Entities
{
    public class KothMatch
    {
        public Guid Id { get; set; }

        public ICollection<KothPlayer> Players { get; set; } = new List<KothPlayer>();

        public KothMatchStatus Status { get; set; } 

        public GameMode SelectedMode { get; set; } 
        
        public QuestionSet? QuestionSet { get; set; } 

        public ICollection<KothAnswer> Answers { get; set; } = new List<KothAnswer>();

        public Guid? WinnerId { get; set; } 
        public User? Winner { get; set; }

        public int CurrentRound { get; set; } 
        public RoundType CurrentRoundType { get; set; } 

        public DateTime CreatedAt { get; set; }
        public DateTime? StartedAt { get; set; }
        public DateTime? FinishedAt { get; set; }
    }
    public enum KothMatchStatus
    {
        Waiting = 1,      
        InGame = 2,   
        Finished = 3      
    }

    public enum RoundType
    {
        Classic = 1,  
        Speed = 2     
    }
}
