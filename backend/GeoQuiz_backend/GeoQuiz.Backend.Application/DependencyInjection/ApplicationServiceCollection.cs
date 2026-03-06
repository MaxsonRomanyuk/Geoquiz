using GeoQuiz.Backend.Application.Interfaces;
using GeoQuiz.Backend.Application.Services;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Configuration;

using GeoQuiz.Backend.Application.Services.KingOfTheHill;
using GeoQuiz.Backend.Application.Services.PvP;

namespace GeoQuiz.Backend.Application.DependencyInjection
{
    public static class ApplicationServiceCollection
    {
        public static IServiceCollection AddApplication(this IServiceCollection services, IConfiguration configuration)
        {
            services.AddScoped<AuthService>();
            services.AddScoped<GameService>();
            services.AddScoped<IAchievementService, AchievementService>();

            services.AddSingleton<MatchmakingQueue>();

            services.AddScoped<IMatchmakingService, MatchmakingService>();
            services.AddScoped<IDraftService, DraftService>();
            services.AddScoped<IQuestionSetService, QuestionSetService>();
            services.AddScoped<IPvPGameSessionService, PvPGameSessionService>();
            services.AddScoped<IPvPResultService, PvPResultService>();

            services.AddScoped<IKothMatchmakingService, KothMatchmakingService>();

            return services;
        }
    }
}
