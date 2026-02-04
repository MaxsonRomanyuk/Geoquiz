using System.ComponentModel.DataAnnotations;

namespace GeoQuiz_backend.Domain.Entities
{
    public class Achievement
    {
        public Guid Id { get; set; }
        public string Code { get; set; } = null!;
        public string Title { get; set; } = null!;
        public string Description { get; set; } = null!;
        public string Icon { get; set; } = null!;
        public AchievementCategory Category { get; set; }
        public AchievementRarity Rarity { get; set; }
        public ICollection<UserAchievement> UserAchievements { get; set; } = new List<UserAchievement>();
    }
    public enum AchievementCategory
    {
        General = 1,     
        Gameplay = 2,    
        Speed = 3,       
        Knowledge = 4,   
        PvP = 5,         
        Streaks = 6     
    }

    public enum AchievementRarity
    {
        Common = 1,      
        Rare = 2,        
        Epic = 3,        
        Legendary = 4    
    }
}
