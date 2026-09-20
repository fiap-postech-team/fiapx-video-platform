import { copy } from '../../product-copy'
import type { Video } from '../domain/video'
import { formatMilestone } from '../application/format-datetime'
import { videoMilestones } from '../application/product-status'

interface VideoTimelineProps {
  video: Video
}

export function VideoTimeline({ video }: VideoTimelineProps) {
  const milestones = videoMilestones(video)

  return (
    <section className="timeline" aria-labelledby="timeline-title">
      <h2 id="timeline-title">{copy.detail.timeline}</h2>
      <ol>
        <li>
          <span>{copy.detail.sent}</span>
          <strong>{formatMilestone(milestones.sent)}</strong>
        </li>
        <li>
          <span>{copy.detail.processed}</span>
          <strong>{formatMilestone(milestones.processed)}</strong>
        </li>
        <li>
          <span>{copy.detail.available}</span>
          <strong>{formatMilestone(milestones.available)}</strong>
        </li>
      </ol>
    </section>
  )
}
