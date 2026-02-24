namespace GeoQuiz_backend.Application.Services.PvP
{
    public class MatchmakingQueue
    {
        private readonly Queue<Guid> _queue = new();
        private readonly HashSet<Guid> _queueSet = new();

        public Guid? Enqueue(Guid userId)
        {
            if (_queue.Count == 0)
            {
                _queue.Enqueue(userId);
                _queueSet.Add(userId);
                return null;
            }

            var opponentId = _queue.Dequeue();
            _queueSet.Remove(opponentId);
            return opponentId;
        }
        public void Remove(Guid userId)
        {
            if (_queueSet.Contains(userId))
            {
                var remaining = _queue.Where(id => id != userId).ToList();
                _queue.Clear();
                _queueSet.Clear();

                foreach (var id in remaining)
                {
                    _queue.Enqueue(id);
                    _queueSet.Add(id);
                }
            }
        }

        public bool Contains(Guid userId)
        {
            return _queueSet.Contains(userId);
        }

        public int Count => _queue.Count;
    }
}
