using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Entities;
using Microsoft.EntityFrameworkCore;
using System.Text.Json;

namespace GeoQuiz_backend.Infrastructure.Persistence.MySQL
{
    public class AppDbContext : DbContext
    {
        public AppDbContext(DbContextOptions<AppDbContext> options)
            : base(options) { }

        public DbSet<User> Users => Set<User>();
        public DbSet<UserStats> UserStats => Set<UserStats>();
        public DbSet<GameSession> GameSessions => Set<GameSession>();
        public DbSet<Subscription> Subscriptions => Set<Subscription>();
        public DbSet<Achievement> Achievements => Set<Achievement>();
        public DbSet<UserAchievement> UserAchievements => Set<UserAchievement>();
        public DbSet<PvPMatch> PvPMatches => Set<PvPMatch>();
        public DbSet<ModeDraft> ModeDrafts => Set<ModeDraft>();
        public DbSet<QuestionSet> QuestionSets => Set<QuestionSet>();
        public DbSet<PvPAnswer> PvPAnswers => Set<PvPAnswer>();
        public DbSet<KothMatch> KothMatches => Set<KothMatch>();
        public DbSet<KothPlayer> KothPlayers => Set<KothPlayer>();
        public DbSet<KothAnswer> KothAnswers => Set<KothAnswer>();

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            modelBuilder.Entity<User>().ToTable("users");
            modelBuilder.Entity<UserStats>().ToTable("userstats");
            modelBuilder.Entity<GameSession>().ToTable("gamesessions");
            modelBuilder.Entity<Subscription>().ToTable("subscriptions");
            modelBuilder.Entity<Achievement>().ToTable("achievements");
            modelBuilder.Entity<UserAchievement>().ToTable("userachievements");
            modelBuilder.Entity<PvPMatch>().ToTable("pvpmatches");
            modelBuilder.Entity<ModeDraft>().ToTable("modedraft");
            modelBuilder.Entity<QuestionSet>().ToTable("questionset");
            modelBuilder.Entity<PvPAnswer>().ToTable("pvpanswers");
            modelBuilder.Entity<KothMatch>().ToTable("kothmatches");
            modelBuilder.Entity<KothPlayer>().ToTable("kothplayers");
            modelBuilder.Entity<KothAnswer>().ToTable("kothanswers");

            modelBuilder.Entity<User>(entity =>
            {
                entity.HasKey(e => e.Id);

                entity.Property(e => e.UserName)
                    .IsRequired()
                    .HasMaxLength(100);

                entity.Property(e => e.Email)
                    .IsRequired()
                    .HasMaxLength(200);

                entity.Property(e => e.PasswordHash)
                    .IsRequired()
                    .HasMaxLength(500);

                entity.Property(e => e.RegisteredAt)
                    .IsRequired();

                entity.HasIndex(e => e.UserName).IsUnique();
                entity.HasIndex(e => e.Email).IsUnique();

                entity.HasOne(e => e.Stats)
                    .WithOne(s => s.User)
                    .HasForeignKey<UserStats>(s => s.UserId)
                    .OnDelete(DeleteBehavior.Cascade);

                entity.HasOne(e => e.Subscription)
                    .WithOne(s => s.User)
                    .HasForeignKey<Subscription>(s => s.UserId)
                    .OnDelete(DeleteBehavior.Cascade);

                entity.HasMany(e => e.GameSessions)
                    .WithOne(gs => gs.User)
                    .HasForeignKey(gs => gs.UserId)
                    .OnDelete(DeleteBehavior.Cascade);

                entity.HasMany(e => e.UserAchievements)
                    .WithOne(ua => ua.User)
                    .HasForeignKey(ua => ua.UserId)
                    .OnDelete(DeleteBehavior.Cascade);
            });

            modelBuilder.Entity<UserStats>(entity =>
            {
                entity.HasKey(e => e.UserId);

                entity.Property(e => e.Level).HasDefaultValue(1);
                entity.Property(e => e.CurrentWinStreak).HasDefaultValue(0);
                entity.Property(e => e.MaxWinStreak).HasDefaultValue(0);
                entity.Property(e => e.DailyLoginStreak).HasDefaultValue(0);
                entity.Property(e => e.LastLoginDate).IsRequired();
                entity.Property(e => e.KothGamesPlayed).HasDefaultValue(0);
                entity.Property(e => e.KothGamesWon).HasDefaultValue(0);
                entity.Property(e => e.KothTop3Finishes).HasDefaultValue(0);

            });

            modelBuilder.Entity<Achievement>(entity =>
            {
                entity.HasKey(e => e.Id);

                entity.Property(e => e.Code)
                    .IsRequired()
                    .HasMaxLength(50);

                entity.HasIndex(e => e.Code).IsUnique();

                entity.Property(e => e.Title)
                    .IsRequired()
                    .HasMaxLength(100);

                entity.Property(e => e.Description).HasMaxLength(500);
                entity.Property(e => e.Icon).IsRequired().HasMaxLength(100);

                entity.Property(e => e.Category)
                    .IsRequired();

                entity.Property(e => e.Rarity)
                    .IsRequired();

                entity.HasMany(e => e.UserAchievements)
                    .WithOne(ua => ua.Achievement)
                    .HasForeignKey(ua => ua.AchievementId)
                    .OnDelete(DeleteBehavior.Cascade);
            });

            modelBuilder.Entity<UserAchievement>(entity =>
            {
                entity.HasKey(e => new { e.UserId, e.AchievementId });

                entity.Property(e => e.UnlockedAt).IsRequired();

            });

            modelBuilder.Entity<GameSession>(entity =>
            {
                entity.HasKey(e => e.Id);

                entity.Property(e => e.Mode)
                    .IsRequired();

                entity.Property(e => e.PlayedAt).IsRequired();

                entity.HasOne(e => e.PvPMatch)
                    .WithMany()
                    .HasForeignKey(e => e.PvPMatchId)
                    .OnDelete(DeleteBehavior.SetNull)
                    .IsRequired(false);

                entity.HasOne(e => e.KothMatch)
                    .WithMany()
                    .HasForeignKey(e => e.KothMatchId)
                    .OnDelete(DeleteBehavior.SetNull)
                    .IsRequired(false);
            });

            modelBuilder.Entity<PvPMatch>(entity =>
            {
                entity.HasKey(e => e.Id);

                entity.Property(e => e.Status)
                    .IsRequired();

                entity.Property(e => e.SelectedMode)
                    .IsRequired(false);

                entity.Property(e => e.CreatedAt).IsRequired();

                entity.HasOne(e => e.Player1)
                    .WithMany()
                    .HasForeignKey(e => e.Player1Id)
                    .OnDelete(DeleteBehavior.Restrict);

                entity.HasOne(e => e.Player2)
                    .WithMany()
                    .HasForeignKey(e => e.Player2Id)
                    .OnDelete(DeleteBehavior.Restrict);

                entity.HasOne(e => e.Winner)
                    .WithMany()
                    .HasForeignKey(e => e.WinnerId)
                    .OnDelete(DeleteBehavior.SetNull)
                    .IsRequired(false);

                entity.HasOne(e => e.Draft)
                    .WithOne(d => d.PvPMatch)
                    .HasForeignKey<ModeDraft>(d => d.PvPMatchId)
                    .OnDelete(DeleteBehavior.Cascade);

                entity.HasOne(e => e.QuestionSet)
                    .WithOne(q => q.PvPMatch)
                    .HasForeignKey<QuestionSet>(q => q.PvPMatchId)
                    .OnDelete(DeleteBehavior.Cascade);


            });

            modelBuilder.Entity<Subscription>(entity =>
            {
                entity.HasKey(e => e.Id);

                entity.Property(e => e.Type)
                    .IsRequired()
                    .HasMaxLength(50);

                entity.Property(e => e.StartDate).IsRequired();
                entity.Property(e => e.EndDate).IsRequired();

            });

            modelBuilder.Entity<ModeDraft>(entity =>
            {
                entity.HasKey(e => e.Id);

                entity.Property(e => e.AvailableModes)
                    .HasConversion(
                        v => JsonSerializer.Serialize(v, (JsonSerializerOptions?)null),
                        v => JsonSerializer.Deserialize<List<GameMode>>(v, (JsonSerializerOptions?)null) ?? new()
                    );

                entity.Property(e => e.BannedModes)
                    .HasConversion(
                        v => JsonSerializer.Serialize(v, (JsonSerializerOptions?)null),
                        v => JsonSerializer.Deserialize<List<GameMode>>(v, (JsonSerializerOptions?)null) ?? new()
                    );

            });

            modelBuilder.Entity<QuestionSet>(entity =>
            {
                entity.HasKey(e => e.Id);

                entity.Property(e => e.Mode)
                    .IsRequired();

                entity.Property(e => e.Language)
                    .IsRequired();

                entity.Property(e => e.QuestionIds)
                    .HasConversion(
                        v => JsonSerializer.Serialize(v, (JsonSerializerOptions?)null),
                        v => JsonSerializer.Deserialize<List<string>>(v, (JsonSerializerOptions?)null) ?? new()
                    );

                entity.HasIndex(e => e.KothMatchId)
                    .IsUnique(false);

                entity.Property(e => e.CreatedAt).IsRequired();
            });

            modelBuilder.Entity<PvPAnswer>(entity =>
            {
                entity.HasKey(e => e.Id);

                entity.Property(e => e.QuestionId)
                    .IsRequired()
                    .HasMaxLength(100);

                entity.Property(e => e.AnsweredAt)
                    .IsRequired();

                entity.Property(e => e.ScoreGained)
                    .IsRequired();

                entity.HasOne(e => e.Match)
                    .WithMany(m => m.Answers)
                    .HasForeignKey(e => e.MatchId)
                    .OnDelete(DeleteBehavior.Cascade);

                entity.HasOne(e => e.User)
                    .WithMany(u => u.PvPAnswers)
                    .HasForeignKey(e => e.UserId)
                    .OnDelete(DeleteBehavior.Cascade);
            });
            modelBuilder.Entity<KothMatch>(entity =>
            {
                entity.HasKey(e => e.Id);

                entity.Property(e => e.Status)
                    .IsRequired();

                entity.Property(e => e.SelectedMode)
                    .IsRequired(false);

                entity.Property(e => e.CurrentRound)
                    .HasDefaultValue(1);

                entity.Property(e => e.CurrentRoundType)
                    .HasDefaultValue(RoundType.Classic);

                entity.Property(e => e.CreatedAt)
                    .IsRequired();

                entity.Property(e => e.StartedAt)
                    .IsRequired(false);

                entity.Property(e => e.FinishedAt)
                    .IsRequired(false);

                entity.HasOne(e => e.Winner)
                    .WithMany()
                    .HasForeignKey(e => e.WinnerId)
                    .OnDelete(DeleteBehavior.SetNull)
                    .IsRequired(false);

                entity.HasOne(e => e.QuestionSet)
                    .WithOne(q => q.KothMatch)
                    .HasForeignKey<QuestionSet>(q => q.KothMatchId)
                    .OnDelete(DeleteBehavior.Cascade)
                    .IsRequired(false);
            });

            modelBuilder.Entity<KothPlayer>(entity =>
            {
                entity.HasKey(e => e.Id);

                entity.Property(e => e.JoinedAt)
                    .IsRequired();

                entity.Property(e => e.Place)
                    .IsRequired(false);

                entity.Property(e => e.IsActive)
                    .HasDefaultValue(true);

                entity.Property(e => e.RoundEliminated)
                    .HasDefaultValue(0);

                entity.HasOne(e => e.Match)
                    .WithMany(m => m.Players)
                    .HasForeignKey(e => e.MatchId)
                    .OnDelete(DeleteBehavior.Cascade);

                entity.HasOne(e => e.User)
                    .WithMany()
                    .HasForeignKey(e => e.UserId)
                    .OnDelete(DeleteBehavior.Cascade);

                entity.HasIndex(e => new { e.MatchId, e.UserId }).IsUnique();
            });

            modelBuilder.Entity<KothAnswer>(entity =>
            {
                entity.HasKey(e => e.Id);

                entity.Property(e => e.QuestionId)
                    .IsRequired()
                    .HasMaxLength(100);

                entity.Property(e => e.RoundNumber)
                    .IsRequired();

                entity.Property(e => e.IsCorrect)
                    .IsRequired();

                entity.Property(e => e.TimeSpentMs)
                    .IsRequired();

                entity.Property(e => e.ScoreGained)
                    .IsRequired();

                entity.Property(e => e.AnsweredAt)
                    .IsRequired();

                entity.HasOne(e => e.Match)
                    .WithMany(m => m.Answers)
                    .HasForeignKey(e => e.MatchId)
                    .OnDelete(DeleteBehavior.Cascade);

                entity.HasOne(e => e.User)
                    .WithMany()
                    .HasForeignKey(e => e.UserId)
                    .OnDelete(DeleteBehavior.Cascade);

                entity.HasIndex(e => new { e.MatchId, e.RoundNumber });
            });
            base.OnModelCreating(modelBuilder);
        }
    }
}
