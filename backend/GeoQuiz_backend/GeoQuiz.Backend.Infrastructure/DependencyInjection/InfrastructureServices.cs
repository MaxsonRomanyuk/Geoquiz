using GeoQuiz.Backend.Application.Interfaces;
using GeoQuiz.Backend.Infrastructure.Mongo;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Configuration;
using GeoQuiz.Backend.Infrastructure.Realtime;

namespace GeoQuiz.Backend.Infrastructure.DependencyInjection
{
    public static class InfrastructureServices
    {
        public static IServiceCollection AddInfrastructure(this IServiceCollection services)
        {
            services.AddScoped<ISignalRNotificationService, SignalRNotificationService>();
            services.AddScoped<IQuestionRepository, QuestionRepository>();
            services.AddScoped<ICountryRepository, CountryRepository>();

            return services;
        }
    }
}
