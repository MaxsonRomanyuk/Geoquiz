namespace GeoQuiz_backend.Application.Services.PvP
{
    public class MatchmakingQueue
    {
        private readonly object _lock = new();
        private readonly Queue<Guid> _queue = new();

        public Guid? Enqueue(Guid userId)
        {
            lock (_lock)
            {
                if (_queue.Count == 0)
                {
                    _queue.Enqueue(userId);
                    return null;
                }

                var opponentId = _queue.Dequeue();
                return opponentId;
            }
        }
        public void Remove(Guid userId)
        {
            lock (_lock)
            {
                var remaining = _queue.Where(id => id != userId).ToList();
                _queue.Clear();

                foreach (var id in remaining)
                {
                    _queue.Enqueue(id);
                }
            }
        }

        public bool Contains(Guid userId)
        {
            lock (_lock)
            {
                return _queue.Contains(userId);
            }
        }

        public int Count => _queue.Count;
    }
}
