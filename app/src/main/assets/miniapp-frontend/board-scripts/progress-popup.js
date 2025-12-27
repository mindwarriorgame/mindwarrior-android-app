function toTimeInterval(secs) {
    let hours = Math.floor(secs / 3600);
    let minutes = Math.floor((secs % 3600) / 60);
    return `${hours}${window.lang.hour_one_letter} ${minutes}${window.lang.minute_one_letter}`;
}

function renderProgressBar(pct, pctDelta) {
    return `<div class="progress-container">
            <div class="progress-bar" id="progress-bar" style="width: ${pct - pctDelta}%;"></div>
            <span class="progress-text" id="progress-text">${pct - pctDelta}%</span>
        </div>`;
}

function renderSimpleTemplate(template, pct, pctDelta, counter, counterIsTime= true)  {
    const ret = [
        '<p>',
        template.replace('###', counterIsTime ? toTimeInterval(counter) : counter),
        '</p>'
    ];
    ret.push(renderProgressBar(pct, pctDelta));
    return ret.join(' ');
}

function wrapWithGrumpyCat(content, hasGrumpyCat) {
    if (hasGrumpyCat) {
        return content + "<br /> <br />" + window.lang.grumpy_cat_no_progress;
    }
    return content;
}

function openPopup(badge, progress, hasGrumpyCat) {
    let content = `<img src="../badge-images/${badge}_512.jpg" />`;

    const timeBadges = {
        'c1': window.lang.review_without_misses_duration,
        'c2': window.lang.review_without_reminders_duration,
        'f0': window.lang.update_formula_after,
        't0': window.lang.play_game,
    };

    const reviewBadges = {
        's0': window.lang.review_without_misses_times,
        's1': window.lang.review_without_misses_times,
        's2': window.lang.review_without_misses_times
    }

    if (timeBadges[badge]) {
        content += renderSimpleTemplate(wrapWithGrumpyCat(timeBadges[badge], hasGrumpyCat), progress.progress_pct, progress.progress_pct_delta, progress.remaining_time_secs);
    } else if (badge === 'c0') {
        const message = window.lang.review_times
            .replace("##1", Math.ceil(progress.remaining_reviews / 3))
            .replace("##2", Math.ceil(progress.remaining_reviews / 2))
            .replace("##3", progress.remaining_reviews);
        const ret = [
            '<p>',
            message,
            '</p>'
        ];
        ret.push(renderProgressBar(progress.progress_pct || 0, progress.progress_pct_delta || 0));
        content += ret.join(' ');
    } else {
        content += renderSimpleTemplate(wrapWithGrumpyCat(reviewBadges[badge], hasGrumpyCat), progress.progress_pct, progress.progress_pct_delta, progress.remaining_reviews, false);
    }

    content += `<p><button class='action-btn' onclick='closePopup()'>${window.lang.close}</button></p>`;

    document.getElementById('popup').innerHTML = content;

    document.getElementById('popup').classList.add('show');
    document.getElementById('overlay').classList.add('show');

    let pctStart = progress.progress_pct - progress.progress_pct_delta;
    let pctEnd = progress.progress_pct;
    if (pctStart < pctEnd) {
        let intervalId = 0;
        setTimeout(() => {
            const progressBar = document.getElementById('progress-bar');
            const progressText = document.getElementById('progress-text');
            intervalId = setInterval(() => {
                if (pctStart >= pctEnd) {
                    clearInterval(intervalId);
                    return;
                }
                pctStart += 1;
                progressBar.style.width = `${pctStart}%`;
                progressText.innerText = `${pctStart}%`;
            }, 750 / (pctEnd - pctStart));
        }, 10);
    }
}

function closePopup() {
    document.getElementById('popup').classList.remove('show');
    document.getElementById('overlay').classList.remove('show');
}