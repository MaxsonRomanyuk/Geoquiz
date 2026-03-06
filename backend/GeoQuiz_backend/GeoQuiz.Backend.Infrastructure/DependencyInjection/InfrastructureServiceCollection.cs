using GeoQuiz.Backend.Application.Interfaces;
using GeoQuiz.Backend.Infrastructure.Mongo;
using GeoQuiz.Backend.Infrastructure.MySQL;
using GeoQuiz.Backend.Infrastructure.Realtime;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;

namespace GeoQuiz.Backend.Infrastructure.DependencyInjection
{
    public static class InfrastructureServiceCollection
    {
        public static IServiceCollection AddInfrastructure(this IServiceCollection services, IConfiguration configuration)
        {
            services.AddDbContext<AppDbContext>(options =>
                options.UseMySql(
                    configuration.GetConnectionString("DefaultConnection"),
                    ServerVersion.AutoDetect(
                        configuration.GetConnectionString("DefaultConnection")
                    )
                )
            );

            services.AddSingleton<MongoContext>();
            services.AddScoped<MongoSeeder>();

            services.AddScoped<ICountryRepository, CountryRepository>();
            services.AddScoped<IQuestionRepository, QuestionRepository>();

            services.AddScoped<ISignalRNotificationService, SignalRNotificationService>();

            return services;
        }
    }
}
