
class ProgressRegistry {
    constructor() {
        this.progressRepository = window.localStorage.getItem('progress');
        if (this.progressRepository) {
            this.progressRepository = JSON.parse(this.progressRepository);
        } else {
            this.progressRepository = {
                timestamps: [],
                timestampToProgress: {}
            };
        }
    }

    register(level, progress, ts) {
        if (this.progressRepository.timestamps.indexOf(parseInt(ts)) < 0) {
            this.progressRepository.timestamps.push(parseInt(ts));
            this.progressRepository.timestamps.sort(function(a, b) {
                return a - b;
            });
        }
        this.progressRepository.timestampToProgress["" + ts] = {
            level,
            progress: JSON.parse(JSON.stringify(progress))
        };
        this._invalidateOldRecords();
        this._syncWithLocalStorage();
    }

    enrichWithProgressPctDelta(level, progress, ts) {
        const earlierTimestamps = this.progressRepository.timestamps.filter(t => t < parseInt(ts));
        if (earlierTimestamps.length === 0) {
            this.enrichWithDefaults(progress);
            return progress;
        }
        const prevTimestamp = earlierTimestamps[earlierTimestamps.length - 1];
        const prevProgressRec = this.progressRepository.timestampToProgress["" + prevTimestamp];
        if (prevProgressRec.level !== level) {
            this.enrichWithDefaults(progress);
            return progress;
        }
        const prevProgress = prevProgressRec.progress;
        const badges = Object.keys(progress);
        badges.forEach(badge => {
            if (prevProgress[badge]) {
                progress[badge]["progress_pct_delta"] = Math.max(
                    Math.min(progress[badge].progress_pct - prevProgress[badge].progress_pct, progress[badge]["progress_pct"]),
                    0
                );
            } else {
                progress[badge]["progress_pct_delta"] = progress[badge]["progress_pct"];
            }
        });
    }

    _syncWithLocalStorage() {
        window.localStorage.setItem('progress', JSON.stringify(this.progressRepository));
    }

    _invalidateOldRecords() {
        if (this.progressRepository.timestampToProgress.length > 100) {
            const itemsToRemove = this.progressRepository.timestamps.slice(0, this.progressRepository.timestamps.length - 100);
            itemsToRemove.forEach(ts => {
                delete this.progressRepository.timestampToProgress["" + ts];
            });
            this.progressRepository.timestamps = this.progressRepository.timestamps.slice(this.progressRepository.timestamps.length - 100);
        }
    }

    enrichWithDefaults(progress) {
        const badges = Object.keys(progress);
        badges.forEach(badge => {
            progress[badge]["progress_pct_delta"] = progress[badge]["progress_pct"];
        });
    }
}