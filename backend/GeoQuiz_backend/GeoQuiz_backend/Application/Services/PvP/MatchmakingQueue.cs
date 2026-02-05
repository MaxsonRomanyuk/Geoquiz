namespace GeoQuiz_backend.Application.Services.PvP
{
    public class MatchmakingQueue
    {
        private readonly Queue<Guid> _queue = new();

        public Guid? Enqueue(Guid userId)
        {
            if (_queue.Count == 0)
            {
                _queue.Enqueue(userId);
                return null;
            }

            return _queue.Dequeue();
        }
    }
}
