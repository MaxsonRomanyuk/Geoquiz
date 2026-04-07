using GeoQuiz_backend.Domain.Entities;
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
        Knowledge = 3,
        Streaks = 4,
        PvP = 5,
        Koth = 6,
        Special = 7
    }

    public enum AchievementRarity
    {
        Common = 1,      
        Rare = 2,        
        Epic = 3,        
        Legendary = 4    
    }
}
