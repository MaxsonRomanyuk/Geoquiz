using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Services;
using GeoQuiz_backend.Application.Services.KingOfTheHill;
using GeoQuiz_backend.Application.Services.PvP;
using GeoQuiz_backend.API.Hubs;
using GeoQuiz_backend.Infrastructure.Persistence.Mongo;
using GeoQuiz_backend.Infrastructure.Persistence.Mongo.Repositories;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Http.Connections;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using System.Text;
using System.Text.Json.Serialization;

var builder = WebApplication.CreateBuilder(args);

// Controllers
builder.Services.AddControllers();

// CORS
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll", policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyMethod()
              .AllowAnyHeader();
    });
});

// Swagger
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "¬ведите: Bearer {ваш JWT токен}"
    });

    c.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference
                {
                    Type = ReferenceType.SecurityScheme,
                    Id = "Bearer"
                }
            },
            Array.Empty<string>()
        }
    });
});

// SignalR
builder.Services.AddScoped<ISignalRNotificationService, SignalRNotificationService>();
builder.Services.AddSignalR()
    .AddJsonProtocol(options => {
        options.PayloadSerializerOptions.Converters
            .Add(new JsonStringEnumConverter());
    });
builder.Services.AddSignalR(options =>
{
    options.EnableDetailedErrors = true;
    options.KeepAliveInterval = TimeSpan.FromSeconds(15);
    options.ClientTimeoutInterval = TimeSpan.FromSeconds(30);
});

// MySQL
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseMySql(
        builder.Configuration.GetConnectionString("DefaultConnection"),
        ServerVersion.AutoDetect(builder.Configuration.GetConnectionString("DefaultConnection"))
    )
);

// Mongo
builder.Services.AddSingleton<MongoContext>();
builder.Services.AddScoped<MongoSeeder>();
builder.Services.AddScoped<ICountryRepository, CountryRepository>();
builder.Services.AddScoped<IQuestionRepository, QuestionRepository>();
// Auth
builder.Services.AddScoped<AuthService>();

// Game
builder.Services.AddScoped<GameService>();


// Achievement
builder.Services.AddScoped<IAchievementService, AchievementService>();

//PVP
builder.Services.AddSingleton<MatchmakingQueue>();
builder.Services.AddScoped<IMatchmakingService, MatchmakingService>();
builder.Services.AddScoped<IDraftService, DraftService>();
builder.Services.AddScoped<IQuestionSetService, QuestionSetService>();
builder.Services.AddScoped<IPvPGameSessionService, PvPGameSessionService>();
builder.Services.AddScoped<IPvPResultService, PvPResultService>();

//KingOfTheHill
builder.Services.AddScoped<IKothMatchmakingService, KothMatchmakingService>();
builder.Services.AddScoped<IKothGameServiceMain, KothGameServiceMain>();
builder.Services.AddScoped<IKothRoundService, KothRoundService>();
builder.Services.AddScoped<IKothResultService, KothResultService>();
builder.Services.AddScoped<IKothGameService, KothGameService>();


var jwtSettings = builder.Configuration.GetSection("Jwt");
var key = Encoding.UTF8.GetBytes(jwtSettings["Key"]!);

builder.Services.AddAuthentication(options =>
{
    options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
    options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
})
.AddJwtBearer(options =>
{
    options.TokenValidationParameters = new TokenValidationParameters
    {
        ValidateIssuer = true,
        ValidateAudience = true,
        ValidateLifetime = true,
        ValidateIssuerSigningKey = true,
        ValidIssuer = jwtSettings["Issuer"],
        ValidAudience = jwtSettings["Audience"],
        IssuerSigningKey = new SymmetricSecurityKey(key)
    };
});
var app = builder.Build();

using (var scope = app.Services.CreateScope())
{
    var seeder = scope.ServiceProvider.GetRequiredService<MongoSeeder>();
    await seeder.SeedCountriesAsync();
    await seeder.SeedQuestionsAsync();
}
// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.MapHub<PvPHub>("/pvpHub", options =>
{
    options.Transports = HttpTransportType.WebSockets |
                         HttpTransportType.LongPolling;
});
app.MapHub<KothHub>("/kothHub", options =>
{
    options.Transports = HttpTransportType.WebSockets |
                         HttpTransportType.LongPolling;
});

app.UseCors("AllowAll");
app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();
app.MapGet("/", () => "Backend is running");

app.Run("http://0.0.0.0:5238");
