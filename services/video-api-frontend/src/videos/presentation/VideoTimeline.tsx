import { copy } from '../../product-copy'
import { formatMilestone } from '../application/format-datetime'
import { videoMilestones } from '../application/product-status'
import type { VideoDetail } from '../domain/video'

interface VideoTimelineProps {
  video: VideoDetail
}

export function VideoTimeline({ video }: VideoTimelineProps) {
  const milestones = videoMilestones(video)
  const steps = [
    { label: copy.detail.sent, milestone: milestones.sent },
    { label: copy.detail.processed, milestone: milestones.processed },
    { label: copy.detail.available, milestone: milestones.available },
  ]

  return (
    <section className="timeline" aria-labelledby="timeline-title">
      <h2 id="timeline-title">{copy.detail.timeline}</h2>
      <ol>
        {steps.map(({ label, milestone }) => (
          <li key={label} className={`timeline-step is-${milestone.kind}`}>
            <span className="timeline-marker" aria-hidden="true" />
            <div>
              <span>{label}</span>
              <strong>{formatMilestone(milestone)}</strong>
            </div>
          </li>
        ))}
      </ol>
    </section>
  )
}
