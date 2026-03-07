using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Payloads;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;

namespace GeoQuiz_backend.Application.Services.KingOfTheHill
{
    public class KothGameService : IKothGameService
    {
        private readonly AppDbContext _db;
        private readonly IQuestionRepository _questionRepo;
        private readonly ICountryRepository _countryRepo;
        private readonly ISignalRNotificationService _notificationService;
        private readonly ILogger<KothGameService> _logger;

        public KothGameService(
            AppDbContext db,
            IQuestionRepository questionRepo,
            ICountryRepository countryRepo,
            ISignalRNotificationService notificationService,
            ILogger<KothGameService> logger)
        {
            _db = db;
            _questionRepo = questionRepo;
            _countryRepo = countryRepo;
            _notificationService = notificationService;
            _logger = logger;
        }

        public async Task<KothMatch> StartMatchFromLobbyAsync(Guid lobbyId, List<(Guid UserId, string UserName, int Level)> players)
        {
            return new KothMatch();
        }

        public async Task<RoundStartedData?> StartNextRoundAsync(Guid matchId)
        {
            return null;
        }

        public async Task<AnswerResultData> SubmitAnswerAsync(Guid matchId, Guid userId, SubmitAnswerRequest request)
        {
            return new AnswerResultData();
        }

        public async Task<RoundFinishedData> FinishRoundAsync(Guid matchId)
        {
            return new RoundFinishedData();
        }

        public async Task<MatchFinishedData> FinishMatchAsync(Guid matchId)
        {
            return new MatchFinishedData();
        }
        public Task<KothGameState?> GetGameStateAsync(Guid matchId)
        {
            return null;
        }
    }
}
