using GeoQuiz_backend.Application.DTOs.KingOfTheHill;

namespace GeoQuiz_backend.Infrastructure.Factories
{
    public class BotFactory
    {
        private static readonly Random _random = new Random();
        private static readonly string[] _botNames = new[]
        {
            "Bot_Alpha", "Bot_Beta", "Bot_Gamma", "Bot_Delta", "Bot_Epsilon",
            "Bot_Zeta", "Bot_Eta", "Bot_Theta", "Bot_Iota", "Bot_Kappa",
            "Bot_Lambda", "Bot_Mu", "Bot_Nu", "Bot_Xi", "Bot_Omicron",
            "Bot_Pi", "Bot_Rho", "Bot_Sigma", "Bot_Tau", "Bot_Upsilon",
            "Bot_Phi", "Bot_Chi", "Bot_Psi", "Bot_Omega",
            "Bot_Alfred", "Bot_Bernard", "Bot_Charles", "Bot_Donald", "Bot_Edward",
            "Bot_Francis", "Bot_George", "Bot_Henry"
        };

        public static List<PlayerInfo> CreateBots(int count)
        {
            var bots = new List<PlayerInfo>();

            for (int i = 0; i < count; i++)
            {
                var botId = Guid.NewGuid();
                var botLevel = _random.Next(1, 30);

                bots.Add(new PlayerInfo
                {
                    PlayerId = botId,
                    PlayerName = _botNames[_random.Next(_botNames.Length)],
                    PlayerLevel = botLevel,
                    IsBot = true,
                });
            }

            return bots;
        }
    }
}
