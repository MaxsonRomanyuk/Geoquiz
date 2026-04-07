using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Application.Services.Achievement
{
    public class AchievementProgressService : IAchievementProgressService
    {
        private static readonly Dictionary<string, List<ProgressLevel>> _progressLevels = new()
        {
            ["GAMES_PLAYED"] = new()
            {
                new(50, "Новичок", AchievementRarity.Common),
                new(100, "Опытный", AchievementRarity.Rare),
                new(500, "Ветеран", AchievementRarity.Epic),
                new(1000, "Легенда", AchievementRarity.Legendary)
            },
            ["GAMES_WON"] = new()
            {
                new(50, "Восходящая звезда", AchievementRarity.Rare),
                new(250, "Чемпион", AchievementRarity.Epic),
                new(500, "Завоеватель", AchievementRarity.Legendary)
            },
            ["FLAGS"] = new()
            {
                new(25, "Знаток флагов", AchievementRarity.Common),
                new(100, "Эксперт флагов", AchievementRarity.Rare),
                new(500, "Мастер флагов", AchievementRarity.Epic),
                new(1000, "Король флагов", AchievementRarity.Legendary)
            },
            ["CAPITALS"] = new()
            {
                new(25, "Знаток столиц", AchievementRarity.Common),
                new(100, "Эксперт столиц", AchievementRarity.Rare),
                new(500, "Мастер столиц", AchievementRarity.Epic),
                new(1000, "Король столиц", AchievementRarity.Legendary)
            },
            ["OUTLINES"] = new()
            {
                new(25, "Знаток контуров", AchievementRarity.Common),
                new(100, "Эксперт контуров", AchievementRarity.Rare),
                new(500, "Мастер контуров", AchievementRarity.Epic),
                new(1000, "Король контуров", AchievementRarity.Legendary)
            },
            ["LANGUAGES"] = new()
            {
                new(25, "Знаток языков", AchievementRarity.Common),
                new(100, "Эксперт языков", AchievementRarity.Rare),
                new(500, "Мастер языков", AchievementRarity.Epic),
                new(1000, "Король языков", AchievementRarity.Legendary)
            },
            ["EUROPE"] = new()
            {
                new(25, "Исследователь Европы", AchievementRarity.Common),
                new(100, "Эксперт Европы", AchievementRarity.Rare),
                new(500, "Мастер Европы", AchievementRarity.Epic),
                new(1000, "Король Европы", AchievementRarity.Legendary)
            },
            ["ASIA"] = new()
            {
                new(25, "Исследователь Азии", AchievementRarity.Common),
                new(100, "Эксперт Азии", AchievementRarity.Rare),
                new(500, "Мастер Азии", AchievementRarity.Epic),
                new(1000, "Король Азии", AchievementRarity.Legendary)
            },
            ["AFRICA"] = new()
            {
                new(25, "Исследователь Африки", AchievementRarity.Common),
                new(100, "Эксперт Африки", AchievementRarity.Rare),
                new(500, "Мастер Африки", AchievementRarity.Epic),
                new(1000, "Король Африки", AchievementRarity.Legendary)
            },
            ["AMERICA"] = new()
            {
                new(25, "Исследователь Америки", AchievementRarity.Common),
                new(100, "Эксперт Америки", AchievementRarity.Rare),
                new(500, "Мастер Америки", AchievementRarity.Epic),
                new(1000, "Король Америки", AchievementRarity.Legendary)
            },
            ["OCEANIA"] = new()
            {
                new(25, "Исследователь Океании", AchievementRarity.Common),
                new(100, "Эксперт Океании", AchievementRarity.Rare),
                new(500, "Мастер Океании", AchievementRarity.Epic),
                new(1000, "Король Океании", AchievementRarity.Legendary)
            },
            ["WIN_STREAK"] = new()
            {
                new(3, "В огне", AchievementRarity.Common),
                new(5, "Неудержимый", AchievementRarity.Rare),
                new(10, "Легендарная серия", AchievementRarity.Epic),
                new(20, "Божественный", AchievementRarity.Legendary)
            },
            ["DAILY_LOGIN"] = new()
            {
                new(7, "Увлечённый", AchievementRarity.Rare),
                new(30, "Преданный", AchievementRarity.Epic),
                new(100, "Одержимый", AchievementRarity.Legendary)
            },
            ["PVP_GAMES_PLAYED"] = new()
            {
                new(25, "Боец арены", AchievementRarity.Rare),
                new(100, "Гладиатор", AchievementRarity.Epic),
                new(250, "Легенда арены", AchievementRarity.Legendary)
            },
            ["PVP_GAMES_WON"] = new()
            {
                new(10, "Соперник", AchievementRarity.Rare),
                new(50, "Чемпион арены", AchievementRarity.Epic),
                new(100, "Легенда PvP", AchievementRarity.Legendary)
            },
            ["PVP_WIN_STREAK"] = new()
            {
                new(3, "Серия PvP", AchievementRarity.Rare),
                new(5, "Доминатор PvP", AchievementRarity.Epic),
                new(10, "Неприкасаемый", AchievementRarity.Legendary)
            },
            ["KOTH_GAMES_PLAYED"] = new()
            {
                new(25, "Покоритель холмов", AchievementRarity.Rare),
                new(100, "Горный козёл", AchievementRarity.Epic),
                new(250, "Король горы", AchievementRarity.Legendary)
            },
            ["KOTH_GAMES_WON"] = new()
            {
                new(10, "Монарх", AchievementRarity.Epic),
                new(25, "Император", AchievementRarity.Legendary)
            },
            ["KOTH_TOP3"] = new() 
            {
                new(10, "Бронзовая корона", AchievementRarity.Rare),
                new(25, "Серебряный претендент", AchievementRarity.Epic),
                new(50, "Золотой воин", AchievementRarity.Legendary)
            },
            ["WORLD_TRAVELER"] = new()
            {
                new(50, "Путешественник", AchievementRarity.Epic),
                new(100, "Легенда мира", AchievementRarity.Legendary)
            },
            ["ALL_ROUNDER"] = new()
            {
                new(50, "Универсал", AchievementRarity.Epic),
                new(100, "Мастер всего", AchievementRarity.Legendary)
            },
            ["TOTAL_CORRECT"] = new()
            {
                new(100, "Искатель знаний", AchievementRarity.Common),
                new(250, "Коллекционер знаний", AchievementRarity.Rare),
                new(1000, "Мудрый мудрец", AchievementRarity.Epic),
                new(5000, "Энциклопедия", AchievementRarity.Legendary)
            },
            ["LEVEL"] = new()
            {
                new(25, "Мастер", AchievementRarity.Epic),
                new(50, "Великий мастер", AchievementRarity.Legendary)
            }
        };

        public record ProgressLevel(int Target, string Title, AchievementRarity Rarity);
        public List<ProgressLevel> GetProgressLevels(string code)
        {
            return _progressLevels.TryGetValue(code, out var levels) ? levels : new List<ProgressLevel>();
        }
        public ProgressLevel? GetCurrentProgressLevel(string code, int currentValue)
        {
            if (!_progressLevels.TryGetValue(code, out var levels))
                return null;

            return levels
                .Where(l => currentValue >= l.Target)
                .MaxBy(l => l.Target);
        }
        public ProgressLevel? GetNextProgressLevel(string code, int currentValue)
        {
            if (!_progressLevels.TryGetValue(code, out var levels))
                return null;

            return levels
                .Where(l => currentValue < l.Target)
                .MinBy(l => l.Target);
        }
        public List<ProgressLevel> GetNewlyReachedLevels(string code, int oldValue, int newValue)
        {
            if (!_progressLevels.TryGetValue(code, out var levels))
                return new List<ProgressLevel>();

            return levels
                .Where(l => oldValue < l.Target && newValue >= l.Target)
                .OrderBy(l => l.Target)
                .ToList();
        }
    }
}
