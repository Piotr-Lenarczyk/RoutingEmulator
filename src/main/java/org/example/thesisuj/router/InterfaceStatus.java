package org.example.thesisuj.router;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class InterfaceStatus {
	private final AdminState admin;
	private final LinkState link;

	public InterfaceStatus() {
		this.admin = AdminState.UP;
		this.link = LinkState.UP;
	}

	public InterfaceStatus(AdminState admin, LinkState link) {
		this.admin = admin;
		this.link = link;
	}

	/**
	 * Parses an interface status from character codes.
	 *
	 * @param admin Admin state character code
	 * @param link Link state character code
	 * @return InterfaceStatus object
	 */
	public static InterfaceStatus fromChars(char admin, char link) {
		return new InterfaceStatus(
				AdminState.fromCode(admin),
				LinkState.fromCode(link)
		);
	}

	@Override
	public String toString() {
		return admin.getCode() + "/" + link.getCode();
	}
}
