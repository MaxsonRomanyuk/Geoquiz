using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.Payloads.Bots;
using GeoQuiz_backend.Application.Payloads.Koth;
using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Application.Services.Bots
{
    public class BotAnswerService
    {
        private static readonly Random _random = new Random();

        public static BotAnswer GenerateAnswer(PlayerInfo bot, GameQuestion question)
        {
            int responseTimeMs = _random.Next(2000, 9000);
            int selectedIndex = new Random().Next(0, 4);
            bool isCorrect = question.CorrectOptionIndex == selectedIndex;

            return new BotAnswer
            {
                PlayerId = bot.PlayerId,
                SelectedIndex = selectedIndex,
                ResponseTimeMs = responseTimeMs,
                IsCorrect = isCorrect
            };
        }
    }
}
