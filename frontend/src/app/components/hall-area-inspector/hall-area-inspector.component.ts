import {Component, Input} from '@angular/core';
import {HallAreaLayout, HallLayout, SectorLayout} from '../../dtos/hall-layout';

type SidebarTab = 'overview' | 'areas';

@Component({
  selector: 'app-hall-area-inspector',
  templateUrl: './hall-area-inspector.component.html',
  styleUrls: ['./hall-area-inspector.component.scss'],
  standalone: false
})
export class HallAreaInspectorComponent {
  @Input() layout?: HallLayout;
  @Input() selectedArea?: HallAreaLayout;
  @Input() selectedSector?: SectorLayout;
  @Input() activeSidebarTab: SidebarTab = 'overview';
  @Input() unassignedAreaCount = 0;

  @Input() setActiveSidebarTab!: (tab: SidebarTab) => void;
  @Input() selectArea!: (area: HallAreaLayout) => void;
  @Input() updateSelectedAreaType!: (type: HallAreaLayout['type']) => void;
  @Input() updateSelectedSectorName!: (name: string) => void;
  @Input() normalizeSelectedSector!: () => void;
  @Input() normalizeSelectedArea!: () => void;
  @Input() getSelectedAreaMaxWidth!: () => number;
  @Input() getSelectedAreaMaxLength!: () => number;
  @Input() deleteSelectedArea!: () => void;

  @Input() getAreaLabel!: (area: HallAreaLayout) => string;
  @Input() getAreaTypeLabel!: (type: HallAreaLayout['type']) => string;
  @Input() getAreaBadge!: (area: HallAreaLayout) => string;
  @Input() isAreaUnassigned!: (area: HallAreaLayout) => boolean;
}
